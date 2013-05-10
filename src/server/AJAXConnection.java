import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AJAXConnection {
	private Socket socket;

	private int id;
	public int getID() { return id; }

	private List<String> packets = new ArrayList<>();

	public AJAXConnection(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
	}

	public void sendMessage(String message) {
		// TODO: Escape message.
		packets.add("handleMessage('" + message + "');");
	}

	public void sendJoin(String name) {
		packets.add("handleJoin('" + name + "');");
	}

	public void update() {
		if (!packets.isEmpty() && !socket.isClosed()) {
			try {
				PrintStream ps = new PrintStream(socket.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n");
				for (String packet : packets) ps.print(packet);
				packets.clear();
				ps.print("\r\n");
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
