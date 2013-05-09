import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AJAXServer {
	public static void main(String[] args) {
		String wwwRoot = args.length > 0 ? args[0] : ".";

		AJAXServer server = new AJAXServer(8080, wwwRoot);
		while (true) {
			AJAXConnection connection = server.accept();
			if (connection != null) connection.respond();

			// TODO: Use a semaphore for accept so that we do not busy loop.
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
	}

	private List<AJAXConnection> connections = new ArrayList<>();

	public AJAXServer(int port, final String wwwRoot) {
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
							case "/poll":
								synchronized (connections) {
									connections.add(new AJAXConnection(connection));
								}
								break;
							default:
								// FIXME: Disallow resources that would read outside of wwwRoot (i.e. "..").
								PrintStream ps = new PrintStream(connection.getOutputStream());
								File resourceFile = new File(wwwRoot + resource);
								if (resourceFile.canRead()) {
									FileInputStream fin = new FileInputStream(resourceFile);
									ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n");
									while (fin.available() != 0) ps.write(fin.read());
									ps.print("\r\n");
									ps.flush();
									connection.close();
									fin.close();
								} else {
									ps.print("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nSorry that file does not exist.\r\n");
									connection.close();
								}
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
