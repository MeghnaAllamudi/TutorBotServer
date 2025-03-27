import java.io.Serializable; 

enum MessageType {
		DATA,CONNECT, CHAT, IDENTIFIED,EMAILIDENTIFIED,CLOSED; 
	}
public class MessageFormat implements Serializable {
	MessageType type;
	String words;
	
	public static MessageFormat prepareMessage( MessageType msgType, String str ) {
	    MessageFormat msg = new MessageFormat();
	    msg.type = msgType;
	    msg.words = str;
	    
	    return msg;
	}
	
}

