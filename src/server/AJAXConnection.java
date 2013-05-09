import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class AJAXConnection {
	private Socket socket;

	public AJAXConnection(Socket socket) {
		this.socket = socket;
	}

	public void respond() {
		try {
			new PrintStream(socket.getOutputStream()).print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello World!\r\n");
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
