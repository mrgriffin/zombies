import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;

public class AJAXServer {
	public static void main(String[] args) {
		AJAXServer server = new AJAXServer(8080);
	}

	private ServerSocket socket;
	private Socket client;

	public AJAXServer(int port) {
		try {
			this.socket = new ServerSocket(port);
			client = this.socket.accept();
			new PrintStream(client.getOutputStream()).print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello World!");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
