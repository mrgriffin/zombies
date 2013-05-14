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
	String wwwRoot;

	int nextConnectionID = 0;
	private Map<Integer, AJAXConnection> connections = new HashMap<>();

	public AJAXServer(int port, String wwwRoot) {
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

	public synchronized void update(Server server) {
		for (AJAXConnection connection : connections.values()) connection.update(server);
	}

	private synchronized void handleConnection(Socket connection) {
		try {
			InputStream in = connection.getInputStream();

			String method = readUntil(in, ' ');
			String resource = readUntil(in, ' ');
			String protocol = readUntil(in, '\r');
			if (!protocol.equals("HTTP/1.1")) { connection.close(); return; }

			// TODO: Real HTTP header handling.
			Map<String, String> headers = new HashMap<>();
			String header;
			do {
				readUntil(in, '\n');
				header = readUntil(in, '\r');
				int i = header.indexOf(':');
				if (i != -1) headers.put(header.substring(0, i), header.substring(i + 2));
			} while (!header.equals(""));
			readUntil(in, '\n');

			String content = "";
			if (headers.containsKey("Content-Length")) {
				int contentLength = Integer.parseInt(headers.get("Content-Length"));
				StringBuffer sb = new StringBuffer(contentLength);
				for (int i = 0; i < contentLength; i++) sb.append((char)in.read());
				content = sb.toString();
			}

			int id = -1;

			if (headers.containsKey("Cookie")) {
				String[] cookiePairs = headers.get("Cookie").split(";");
				for (String pair : cookiePairs) {
					int i = pair.indexOf('=');
					if (i != -1 && pair.substring(0, i).equals("id"))
						id = Integer.parseInt(pair.substring(i + 1));
				}
			}

			// TODO: Rather than close the connection on failure we should return a 4XX code.

			switch (resource) {
			case "/join": {
				if (!method.equals("POST")) { connection.close(); return; }
				if (!connections.containsKey(id)) return;
				AJAXConnection ajaxConnection = connections.get(id);
				PrintStream ps = new PrintStream(connection.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nack.\r\n");
				connection.close();
				ajaxConnection.recvJoin(content);
				break;
			} case "/state": {
				if (!method.equals("POST")) { connection.close(); return; }
				if (!connections.containsKey(id)) return;
				AJAXConnection ajaxConnection = connections.get(id);
				String[] parts = content.split(",");
				if (parts.length != 3) { connection.close(); return; }
				double[] state = new double[2];
				for (int i = 0; i < 2; ++i) state[i] = Double.parseDouble(parts[i]);
				boolean rangedAttack = Boolean.parseBoolean(parts[2]);
				PrintStream ps = new PrintStream(connection.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nack.\r\n");
				connection.close();
				ajaxConnection.recvState(state[0], state[1], rangedAttack);
				break;
			// TODO: Add a /connect resource that initializes the AJAXConnection.
			} case "/update": {
				if (!method.equals("GET")) { connection.close(); return; }
				// TODO: It should be an error for id to be -1; cookies must be off.
				if (id == -1 || !connections.containsKey(id)) {
					AJAXConnection ajaxConnection = new AJAXConnection(connection, id);
					connections.put(id, ajaxConnection);
				} else {
					AJAXConnection ajaxConnection;
					ajaxConnection = connections.get(id);
					ajaxConnection.setSocket(connection);
				}
				break;
			} default: {
				if (!method.equals("GET")) { connection.close(); return; }
				handleStaticConnection(connection, id, resource);
				break;
			}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleStaticConnection(Socket connection, int id, String resource) {
		String mimeType;
		String headers = "";
		switch (resource) {
		case "/": resource = "/index";
		case "/index":
			mimeType = "text/html";
			headers = "Set-Cookie: id=" + nextConnectionID++ + "\r\n";
			break;
		case "/boss.ogg":
		case "/build-up.ogg":
		case "/hmatch.ogg":
			mimeType = "audio/ogg";
			break;
		case "/three.min.js":
			mimeType = "application/javascript";
			headers = "Cache-Control: max-age=3600\r\n";
			break;
		default:
			handleNotFound(connection, resource);
			return;
		}
		try {
			PrintStream ps = new PrintStream(connection.getOutputStream());
			File resourceFile = new File(wwwRoot + resource);
			if (resourceFile.canRead()) {
				FileInputStream fin = new FileInputStream(resourceFile);
				ps.print("HTTP/1.1 200 OK\r\n");
				ps.print("Content-Type: " + mimeType + "\r\n");
				ps.print(headers);
				ps.print("\r\n");
				while (fin.available() != 0) ps.write(fin.read());
				ps.print("\r\n");
				ps.flush();
				connection.close();
				fin.close();
			} else {
				handleNotFound(connection, resource);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleNotFound(Socket connection, String resource) {
		try {
			PrintStream ps = new PrintStream(connection.getOutputStream());
			ps.print("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nSorry " + resource + " was not found.\r\n");
			ps.flush();
		} catch (IOException e) {
		} finally {
			try { connection.close(); } catch (IOException e) {}
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
