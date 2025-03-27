import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;
import javax.swing.*; 

public class Server extends JFrame{
	
	private JTextField userText; 
	private JTextArea chatWindow; 
	private ServerSocket server; 
	private Socket client; 
	private ExecutorService pool = null; 
	private HashMap<String,ObjectOutputStream> threadIdentifiers; 
	HashMap<Subject,ArrayList<UserNameAndAvailability>> students; 
	HashMap<Subject,ArrayList<UserNameAndAvailability>> tutors; 
	
	private Object studentLock = new Object(); 
	private Object tutorLock = new Object(); 
	
	
	
	int clientcount = 0; 
	
	enum Status {
		BUSY,AVAILABLE;
	}
	
	enum Subject{
		MATH,SCIENCE,ENGLISH,HISTORY;
	}
	enum Role{
		STUDENT, TUTOR; 
	}
	enum CurrentState {
		NEW,IDENTIFIED,GOTNAME,WAITFORSELECTION,CONNECTED; 
	}
	

	
	public Server() {
		super("Tutor Bot"); 
		userText = new JTextField(); 
		userText.setEditable(false);
		add(userText,BorderLayout.NORTH); 
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		setSize(300,150);
		setVisible(true);
		threadIdentifiers = new HashMap<String,ObjectOutputStream>(); 
		pool = Executors.newFixedThreadPool(100);
		students = new HashMap<Subject,ArrayList<UserNameAndAvailability>>();
		tutors = new HashMap<Subject,ArrayList<UserNameAndAvailability>>();
		
		
	}
	public void startRunning() {
		try {
			server = new ServerSocket(6789,100);
			while(true) {
				try {
					waitForConnection();
				}catch(EOFException eofException) {
					showMessage("\n Server ended the connection");
				}
			}
		}catch(IOException ioException) {
			ioException.printStackTrace(); 
		}
	}
	
	public void waitForConnection() throws IOException {
		showMessage("\nWaiting for someone to connect...");
		client = server.accept(); 
		showMessage("Got connection from: " + client.getInetAddress().getHostName());
		clientcount++;
		ServerThread runnable = new ServerThread(client,clientcount,this);
		pool.execute(runnable);
		
		
	}
	
	private void showMessage(final String text) {
		SwingUtilities.invokeLater(
				//thread
				new Runnable() {
					public void run() {
						chatWindow.append(text);
					}
				}
			);
	
	}
	
	class UserNameAndAvailability {
		String tutorName; 
		Status status; 
		
		public UserNameAndAvailability(String name,Status stat) {
			this.tutorName = name; 
			this.status = stat; 
		}
	}
	
	private class ServerThread implements Runnable{
		
		Role role; 
		String emailAdd;
		String partnerAdd;
		Subject subject; 
		Server server = null; 
		Socket client; 
		int chatID; 
		Student student; 
		int index; 
		Tutor tutor; 
		boolean isAsleep;
		CurrentState currentState; 
		ObjectOutputStream cout;
		ObjectInputStream cin; 
		
		
		ArrayList<String> responses; 
		ArrayList<String> questions; 
		ServerThread(Socket client, int count, Server server) throws IOException {
			this.client=client; 
			this.server=server; 
			this.chatID=count; 
			index = 0; 
			currentState = CurrentState.NEW; 
			showMessage("\nconnection made\n");
			responses = new ArrayList<String>(); 
			questions = new ArrayList<String>(); 
			questions.add("What is your first name?\n"); 
			isAsleep = false; 
			//os = new DataOutputStream(client.getOutputStream());
			
		}

		@Override
		public void run() {
			try {
				//while(true) {
					try {
						setupStream();
						whileChatting();
					}catch(EOFException eofException) {
						showMessage("\n Server ended the connection");
					}finally {
						closeStream(); 
					}
					
					
				//}
			}catch(IOException ioException) {
				ioException.printStackTrace(); 
			}
				
		}
		public void setupStream() throws IOException {
			cout = new ObjectOutputStream(client.getOutputStream());
			cout.flush(); 
			cin = new ObjectInputStream(client.getInputStream());
			showMessage("\nStreams are now setup \n");
			
		}
		public void checkIfWindowClosed(MessageFormat msg) {
			if(msg.type == MessageType.CLOSED) {
				MessageFormat mf = MessageFormat.prepareMessage(MessageType.DATA, msg.words);
				try {
					threadIdentifiers.get(partnerAdd).writeObject(mf);
					threadIdentifiers.get(partnerAdd).flush();
				} catch (IOException e) {
					System.out.println("Could not send message");
				}
				
				if(role == Role.TUTOR) {
					for( UserNameAndAvailability tna : tutors.get(subject)) {
						if(tna.tutorName.equals(emailAdd)) {
							tutors.get(subject).remove(tna);
							break; 
						}
					}
					currentState = CurrentState.IDENTIFIED;
					return;
					
				} else if(role == Role.STUDENT) {
					for( UserNameAndAvailability tna : students.get(subject)) {
						if(tna.tutorName.equals(emailAdd)) {
							students.get(subject).remove(tna);
							break; 
						}
					}
				}
			}
		}
		public void whileChatting() throws IOException {
			String message = "Hello, I am your very own Tutor Bot.\nFirst, I need to know if you are a student or a tutor, please type s or t.\n";
			sendMessage(message); 
			while(true) {
				try {
					MessageFormat msg = ( MessageFormat ) cin.readObject();
					message = msg.words;
					
					//message = message.substring(message.indexOf(':') + 2); 
					processMessage(message);
					checkIfWindowClosed(msg);
					
					
				} catch(ClassNotFoundException classNotFound) {
					showMessage("\n weird object recieved "); 
					sendMessage("I'm sorry, I didn't quite get that"); 
				}
			}
			
			
		}
			
		private boolean establishIdentity(String message) {
			if(!message.equals("s") && !message.equals("t")){
				sendMessage("This is an invalid identity\n ");
				return false; 
			} 
			if(message.equals("s")) {
				role = Role.STUDENT; 
			} else {
				role = Role.TUTOR; 
			}
			return true; 
			
			
		}
		
		private boolean establishName(String message) {
			
			emailAdd = message; 
			return true; 
		}
		
		private boolean establishSubject(String message) {
			if(message.equals("Math")) {
				subject = Subject.MATH; 
				return true; 
			} else if(message.equals("Science")) {
				subject = Subject.SCIENCE; 
				return true; 
			}else if(message.equals("English")) {
				subject = Subject.ENGLISH; 
				return true; 
			}else if(message.equals("History")) {
				subject = Subject.HISTORY;
				return true; 
			}
		
			return false; 
		}
	
		private void closeStream() {
			showMessage("\nclosing connection\n");
			//ableToType(false);
			try {
				cin.close();
				cout.close();
				client.close();
			}catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
		private void sendMessage( String message) {
			try {
				MessageFormat msg = MessageFormat.prepareMessage( MessageType.DATA, 
						            "TUTORBOT: " + message );
				cout.writeObject(msg);
				cout.flush(); 
				showMessage("\nTUTORBOT: " + message); 
			}catch(IOException ioException) {
				chatWindow.append("\nERROR: I can't send that message");
			}
		}
		
		
		public boolean updateStudent(Student student) {
			synchronized(studentLock) {
				if(!students.containsKey(subject)) {
			    	ArrayList<UserNameAndAvailability> studentsOnline = new ArrayList<UserNameAndAvailability >(); 
			    	UserNameAndAvailability tna = new UserNameAndAvailability(student.FirstName,Status.AVAILABLE);
			    	studentsOnline.add(tna);
			    	students.put(subject, studentsOnline);
			    	threadIdentifiers.put(student.FirstName, cout);
			    	studentLock.notify();
			    	return true; 
			    }else if(students.containsKey(subject)) {
			     	UserNameAndAvailability tna = new UserNameAndAvailability(student.FirstName,Status.AVAILABLE);
			    	if(!students.get(subject).contains(tna)) {
			    		
			    		students.get(subject).add(tna);
			    		threadIdentifiers.put(student.FirstName, cout);
			    		studentLock.notify();
			    		return true; 
			    	} else {
			    		studentLock.notify(); 
			    		return false; 
			    	}
			    }
			}
			studentLock.notify();
			return false; 
		}
		public boolean updateTutor(Tutor tutor) {
			synchronized(tutorLock) {
				if(!tutors.containsKey(subject)) {
			    	ArrayList<UserNameAndAvailability> tutorsOnline = new ArrayList<UserNameAndAvailability>(); 
			    	UserNameAndAvailability tna = new UserNameAndAvailability(tutor.FirstName,Status.AVAILABLE);
			    	tutorsOnline.add(tna);
			    	tutors.put(subject,tutorsOnline);
			    	threadIdentifiers.put(tutor.FirstName,cout);
			    	tutorLock.notify();
			    	return true; 
			    }else if(tutors.containsKey(subject)) {
			    	UserNameAndAvailability tna = new UserNameAndAvailability(tutor.FirstName,Status.AVAILABLE);
			    	if(!tutors.get(subject).contains(tna)) {
			    		tutors.get(subject).add(tna);
				    	threadIdentifiers.put(tutor.FirstName,cout);
			    		tutorLock.notify();
			    		return true; 
			    	} else {
			    		tutorLock.notify();
			    		return false; 
			    	}
			    }
				tutorLock.notify();
				return false; 
			}
			
		}
		
		public boolean storeUser() {
			if(role == Role.STUDENT) {
				student = new Student("s",emailAdd,subject.toString());
				return updateStudent(student); 
			   
			} else if(role == Role.TUTOR) {
				tutor = new Tutor("t",emailAdd,subject.toString());
				return updateTutor(tutor); 
			    
			}
			return false; 
		}
		
		public ArrayList<String> matchTutor() {
			synchronized (tutorLock) {
				while(tutors.get(subject) == null) {
					try {
						tutorLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			ArrayList<String> tutorMatches = new ArrayList<String>(); 
			for(UserNameAndAvailability tna : tutors.get(subject)) {
				if(tna.status == Status.AVAILABLE) {
					tutorMatches.add(tna.tutorName);
				}
			}
			return tutorMatches; 
			
		}
		public ArrayList<String> getPeopleToConnectWith() {
			if(role == Role.STUDENT) {
				return matchTutor(); 
			} /*else if(role == Role.TUTOR) {
				return matchStudent();
			} */
			return new ArrayList<String>();
		}
		
		@SuppressWarnings("unlikely-arg-type")
        public boolean validateSelection(String message) {
			if(role == Role.STUDENT) {
				for( UserNameAndAvailability tna : tutors.get(subject)) {
					System.out.println("TUTORS LIST: " + tna.tutorName + " " + tna.status.toString()); 
					if(tna.tutorName.equals(message) && tna.status == Status.AVAILABLE) {
						return true; 
					}
				}
			} else if(role == Role.TUTOR) {
				if(students.get(subject).contains(message)) {
					return true; 
				}
			} return false; 
		}
		
		public void updateTutorStatus(String message) {
			System.out.println("MADE IT TO THIS UPDATE TUTOR METHOD");
			System.out.println("EMAILADD: " + emailAdd);
			System.out.println("MESSAGE: " + message);
			synchronized(tutorLock) {
				for(UserNameAndAvailability tna : tutors.get(subject)) {
					if(tna.tutorName.equals(message)) {
						tna.status = Status.BUSY;
						System.out.println("TUTORS LIST 2: " + tna.tutorName + " " + tna.status.toString()); 
						break; 
					}
				}
				for(UserNameAndAvailability tna : students.get(subject)) {
					if(tna.tutorName.equals(emailAdd)) {
						tna.status = Status.BUSY;
						System.out.println("STUDENTS LIST: " + tna.tutorName + " " + tna.status.toString()); 
						break; 
					}
				}
			}
			
		}
		public boolean establishConnection(String message) {
			try {
				if(!threadIdentifiers.containsKey(message)) return false; 
				updateTutorStatus(message); 
				
				MessageFormat msg = MessageFormat.prepareMessage( MessageType.CONNECT, emailAdd);
				threadIdentifiers.get(message).writeObject( msg );
				threadIdentifiers.get(message).flush();
				msg = MessageFormat.prepareMessage( MessageType.DATA, 
						                "TutorBot is now connecting you and \n" + emailAdd);
				threadIdentifiers.get(message).writeObject(msg);
				threadIdentifiers.get(message).flush();
				
				partnerAdd = message;
					
			} catch (IOException e) {
				sendMessage("could not send message"); 
				return false; 
			}
			
			return true; 
			
		}
		
		
		
		
		private void processMessage(String message) {
			switch(currentState) {
			case NEW: 
				System.out.println("New stage reached and MESSAGE: " + message);
				if( !establishIdentity(message)) {
					closeStream();
					return;
				}
				MessageFormat identifyingMsg = MessageFormat.prepareMessage(MessageType.IDENTIFIED,message);
				try {
					cout.writeObject(identifyingMsg);
				}catch(IOException ioException) {
					sendMessage("Could not establish identity"); 
				}
				currentState = CurrentState.IDENTIFIED;
				sendMessage("What is your email?"); 
				break; 
			case IDENTIFIED: 
				System.out.println("Identified stage role: " + role.toString() + " MESSAGE: " + message);
				System.out.println("The " + role.toString() + " has made it here");
				if(!establishName(message)) {
				   closeStream(); 
				   return; 
			    }
				MessageFormat namemsg = MessageFormat.prepareMessage(MessageType.EMAILIDENTIFIED, message);
				try {
					cout.writeObject(namemsg);
				}catch(IOException ioException) {
					sendMessage("could not chang title of window");
				}
				currentState = CurrentState.GOTNAME; 
				sendMessage("What subject are you interested in?\n");
				break;
			case GOTNAME: 
				System.out.println("GotName stage role: " + role.toString() + " MESSAGE: " + message);
				System.out.println("The " + role.toString() + " has made it here");
				if(!establishSubject(message)) {
					closeStream();
					return;
				}
				sendMessage("We will now store your information and try to find you a someone to chat with\n"); 
				if(!storeUser()) {
					sendMessage("We could not store you in database\n");
					closeStream(); 
					return; 
				}
				sendMessage("We stored you in database\n");
				if(role == Role.STUDENT) {
					sendMessage("We are now looking for tutors for you to connect with\n");
					sendMessage("We could not find people to connect with\n");
					System.out.println("NUMBER OF PPL: " + getPeopleToConnectWith().size());
					sendMessage("Here are the names of the people you can connect with: " + getPeopleToConnectWith().toString());
					sendMessage("\nWho would you like to work with?\n");
					currentState = CurrentState.WAITFORSELECTION;
					return; 
				}
				currentState = CurrentState.WAITFORSELECTION; 
				break;
			case WAITFORSELECTION: 
				System.out.println("WaitforSelection stage role: " + role.toString() + " MESSAGE: " + message);
				System.out.println("The " + role.toString() + " has made it here");
				if(role == Role.STUDENT) {
					if (!validateSelection(message)) {
						sendMessage("I'm sorry, that tutor was not part of the list provided.\n");
						sendMessage("Here is the list of tutors, please pick from these: " + getPeopleToConnectWith().toString());
						currentState = CurrentState.WAITFORSELECTION; 
						return; 
					}
					sendMessage("Great! I will now send this person a notification that you would like to connect");
				if(!establishConnection(message)) {
					sendMessage("Connection could not be established\n");
				}
			} else {
				partnerAdd = message;
			}
				currentState = CurrentState.CONNECTED; 
				break;
			case CONNECTED:
				// Echo all messages to partner in this state.
				try {
					MessageFormat msg = MessageFormat.prepareMessage( MessageType.DATA, emailAdd + ": " + message );
					threadIdentifiers.get(partnerAdd).writeObject( msg );
					threadIdentifiers.get(partnerAdd).flush();
			
				} catch (IOException e) {
					sendMessage("could not send message"); 
					return;
				}
				break;
				
				
		}
		
	}
		
		
		
		
			
	}
	
	
	
}

