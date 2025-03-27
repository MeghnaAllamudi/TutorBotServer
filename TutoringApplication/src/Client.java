import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Client extends JFrame {
	
	private JTextField userText; 
	private JTextArea chatWindow; 
	private ObjectOutputStream output; 
	private ObjectInputStream input; 
	private String message = ""; 
	private String serverIP; 
	private Socket connection; 
	private String userIdentity; 
	
	

	
	public Client(String host) {
		super("You");
		serverIP = host; 
		userText = new JTextField(); 
		userText.setEditable(false);
		userText.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						sendMessage(MessageType.DATA, event.getActionCommand());
						userText.setText("");
					}
				}
			);
			add(userText,BorderLayout.NORTH);
			chatWindow = new JTextArea();
			add(new JScrollPane(chatWindow));
			setSize(500,500);
			setVisible(true);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					if(userIdentity.equals("Student")) {
						MessageFormat closingMsg = MessageFormat.prepareMessage(MessageType.CLOSED, "Student is leaving!");
						try{
							output.writeObject(closingMsg);
						}catch(IOException ioException) {
							System.out.println("Could not notify partner");
						}
					} else if(userIdentity.equals("Tutor")) {
						MessageFormat closingMsg = MessageFormat.prepareMessage(MessageType.CLOSED, "Tutor is leaving!");
						try{
							output.writeObject(closingMsg);
						}catch(IOException ioException) {
							System.out.println("Could not notify partner");
						}
					}
				}
				public void windowClosed(WindowEvent e) {
					//code
				}
			});
		
	}
	public void startRunning() {
		try {
			connectToServer();
			setupStreams();
			whileChatting(); 
		}catch(EOFException eofException) {
			showMessage("\n You have left the conversation"); 
			
		}catch(IOException ioException) {
			ioException.printStackTrace();
		}finally {
			closeStream();
		}
	}
	
	private void connectToServer() throws IOException{
		showMessage("Attempting connection...\n");
		connection = new Socket(InetAddress.getByName(serverIP), 6789); 
		showMessage("connected to: " + connection.getInetAddress().getHostName()); 
		
		
	}
	private void setupStreams() throws IOException{
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("\n Streams are good to go! \n");
		
		
	}
	private void whileChatting() throws IOException{
		ableToType(true); 
		do {
			try {
				MessageFormat message;
				message = (MessageFormat) input.readObject();
				if(message.type == MessageType.DATA) {
					showMessage("\n" + message.words);
				} else if( message.type == MessageType.CONNECT) {
				  sendMessage( MessageType.CONNECT, message.words );
				} else if(message.type == MessageType.IDENTIFIED) {
					if(message.words.equals("s")) {
						setTitle("Student");
						userIdentity = "Student";
					} else {
						setTitle("Tutor");
						userIdentity = "Tutor";
					}
					
				} else if(message.type == MessageType.EMAILIDENTIFIED) {
					setTitle(message.words);
				}
				 
			}catch(ClassNotFoundException classNotFoundException) {
				showMessage("\n Don't know that object type");
			}
		}while(!message.equals("TUTORBOT: END"));
		
	}
	private void closeStream() {
		showMessage("\n closing connection down");
		ableToType(false); 
		try {
			output.close();
			input.close();
			connection.close(); 
		}catch(IOException ioException) {
			ioException.printStackTrace();
		}
	}
	private void sendMessage(MessageType type, String message) {
		try {
			MessageFormat msg = MessageFormat.prepareMessage(type, message);
			output.writeObject(msg); 
			output.flush();
			showMessage("\nYOU: " + message); 
			
		}catch(IOException ioException) {
			chatWindow.append("\n something went wrong sending message\n");
		}
	}
	private void showMessage(final String message) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						chatWindow.append(message);
					}
				}
			);
	}
	private void ableToType(final boolean tof) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						userText.setEditable(tof);
					}
				}
			);
	}

}
