import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class AJAXConnection {
	private Socket socket;

	private int id;
	public int getID() { return id; }

	public AJAXConnection(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
	}

	public void respond() {
		try {
			PrintStream ps = new PrintStream(socket.getOutputStream());
			ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nPong.\r\n");
			ps.flush();
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
