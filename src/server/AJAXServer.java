import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AJAXServer {
	public static void main(String[] args) {
		AJAXServer server = new AJAXServer(8080);
		while (true) {
			AJAXConnection connection = server.accept();
			if (connection != null) connection.respond();

			// TODO: Use a semaphore for accept so that we do not busy loop.
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
	}

	private List<AJAXConnection> connections = new ArrayList<>();

	public AJAXServer(int port) {
		try {
			final ServerSocket socket = new ServerSocket(port);
			Thread accepter = new Thread() {
				public void run() {
					while (true) {
						try {
							Socket connection = socket.accept();
							InputStream in = connection.getInputStream();

							String method = readUntil(in, ' ');
							String resource = readUntil(in, ' ');
							String protocol = readUntil(in, '\r');
							assert(method.equals("GET"));
							assert(protocol.equals("HTTP/1.1"));

							switch (resource) {
							default:
								synchronized (connections) {
									connections.add(new AJAXConnection(connection));
								}
								break;
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			};
			accepter.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AJAXConnection accept() {
		synchronized (connections) {
			if (connections.isEmpty()) return null;
			else return connections.remove(0);
		}
	}

	private static String readUntil(InputStream in, char u) {
		StringBuffer sb = new StringBuffer();
		int c;
		try {
			while ((c = in.read()) != -1 && c != u) sb.append((char)c);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}
}
