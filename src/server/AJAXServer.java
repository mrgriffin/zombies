import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AJAXServer {
	Server server;
	String wwwRoot;

	int nextConnectionID = 0;
	private Map<Integer, AJAXConnection> connections = new HashMap<>();
	private List<AJAXConnection> newConnections = new ArrayList<>();

	public AJAXServer(Server server, int port, String wwwRoot) {
		this.server = server;
		this.wwwRoot = wwwRoot;
		try {
			final ServerSocket socket = new ServerSocket(port);
			new Thread() {
				public void run() {
					while (true) {
						try {
							handleConnection(socket.accept());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleConnection(Socket connection) {
		try {
			InputStream in = connection.getInputStream();

			String method = readUntil(in, ' ');
			String resource = readUntil(in, ' ');
			String protocol = readUntil(in, '\r');
			if (!method.equals("GET")) { connection.close(); return; }
			if (!protocol.equals("HTTP/1.1")) { connection.close(); return; }

			// TODO: Real HTTP header handling.
			String cookie;
			do {
				readUntil(in, '\n');
				cookie = readUntil(in, '\r');
			} while (cookie.indexOf("Cookie: ") != 0 && !cookie.equals(""));

			int id = -1;

			if (!cookie.equals("")) {
				String[] cookiePairs = cookie.substring(8).split(";");
				for (String pair : cookiePairs) {
					int i = pair.indexOf('=');
					if (i != -1 && pair.substring(0, i).equals("id"))
						id = Integer.parseInt(pair.substring(i + 1));
				}
			}

			switch (resource) {
			case "/ping":
				server.handlePing(id);
				PrintStream ps = new PrintStream(connection.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nPing.\r\n");
				connection.close();
				break;
			case "/pong":
				// TODO: It should be an error for id to be -1; cookies must be off.
				if (id == -1 || !connections.containsKey(id)) {
					AJAXConnection ajaxConnection = new AJAXConnection(connection, id);
					synchronized (connections) { connections.put(id, ajaxConnection); }
					synchronized (newConnections) { newConnections.add(ajaxConnection); }
				} else {
					AJAXConnection ajaxConnection;
					synchronized (connections) { ajaxConnection = connections.get(id); }
					ajaxConnection.setSocket(connection);
				}
				break;
			default:
				handleStaticConnection(connection, id, resource);
				break;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleStaticConnection(Socket connection, int id, String resource) {
		try {
			// FIXME: Disallow resources that would read outside of wwwRoot (i.e. "..").
			PrintStream ps = new PrintStream(connection.getOutputStream());
			File resourceFile = new File(wwwRoot + resource);
			if (resourceFile.canRead()) {
				FileInputStream fin = new FileInputStream(resourceFile);
				ps.print("HTTP/1.1 200 OK\r\n");
				ps.print("Content-Type: text/html\r\n");
				if (id == -1) ps.print("Set-Cookie: id=" + nextConnectionID++ + "\r\n");
				ps.print("\r\n");
				while (fin.available() != 0) ps.write(fin.read());
				ps.print("\r\n");
				ps.flush();
				connection.close();
				fin.close();
			} else {
				ps.print("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nSorry that file does not exist.\r\n");
				ps.flush();
				connection.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AJAXConnection accept() {
		synchronized (newConnections) {
			if (newConnections.isEmpty()) return null;
			else return newConnections.remove(0);
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
