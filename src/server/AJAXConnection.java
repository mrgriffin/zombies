import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AJAXConnection {
	private Socket socket;

	private int id;
	public int getID() { return id; }

	private List<String> messages = new ArrayList<>();

	public AJAXConnection(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
	}

	public void sendMessage(String message) {
		messages.add(message);
	}

	public void update() {
		if (!messages.isEmpty() && !socket.isClosed()) {
			try {
				PrintStream ps = new PrintStream(socket.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n" + messages.remove(0) + "\r\n");
				ps.flush();
				socket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	void setSocket(Socket socket) {
		this.socket = socket;
	}
}
