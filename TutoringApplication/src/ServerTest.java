import javax.swing.JFrame;

public class ServerTest {
	public static void main(String[] args) {
		Server tutorBot = new Server(); 
		tutorBot.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		tutorBot.startRunning(); 
	}
}

