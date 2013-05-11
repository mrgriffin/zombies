import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
	public static void main(String[] args) {
		new Server(8080, args.length > 0 ? args[0] : ".");
	}

	private Map<AJAXConnection, String> chatters = new HashMap<>();

	public Server(int port, String wwwRoot) {
		AJAXServer server = new AJAXServer(this, port, wwwRoot);
		while (true) {
			synchronized (chatters) { for (AJAXConnection connection : chatters.keySet()) connection.update(); }
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
	}

	void handleJoin(AJAXConnection connection, String name) {
		synchronized (chatters) {
			if (!chatters.containsKey(connection)) {
				chatters.put(connection, name);
				for (AJAXConnection connection_ : chatters.keySet()) connection_.sendJoin(chatters.get(connection));
			}
		}
	}

	void handleChat(AJAXConnection connection, String message) {
		synchronized (chatters) {
			if (!chatters.containsKey(connection)) return;
			for (AJAXConnection connection_ : chatters.keySet()) connection_.sendMessage(chatters.get(connection) + ": " + message);
		}
	}
}
