import javax.swing.JFrame;

public class ClientTest {

	public static void main(String[] args) {
		Client TutorOrStudent = new Client("127.0.0.1");
		TutorOrStudent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		TutorOrStudent.startRunning();
	}
}

