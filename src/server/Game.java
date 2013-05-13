import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Contact {
	double x, y;
	public Contact(double x, double y) { this.x = x; this.y = y; }
}

public class Game {
	private List<Wall> walls = new ArrayList<>();
	private List<Player> players = new ArrayList<>();
	private List<Player> enemies = new ArrayList<>();
	private Map<Player, Integer> enemyIDs = new HashMap<>();
	private int enemyID = 0;

	public void addPlayer(Player player) {
		players.add(player);
	}

	public void addEnemy(Player enemy) {
		enemies.add(enemy);
		enemyIDs.put(enemy, enemyID++);
	}

	public void addWall(Wall wall) {
		walls.add(wall);
	}

	public void update(double dt) {
		for (Player player : players) player.update(dt);
		for (Player enemy : enemies) { setAIInputs(enemy); enemy.update(dt); }

		for (int i = 0; i < players.size(); ++i) {
			Player pi = players.get(i);

			for (int j = i + 1; j < players.size(); ++j) {
				Player pj = players.get(j);
				Contact c = circleCircleIntersection(pi.x, pi.y, 12, pj.x, pj.y, 12);
				if (c != null) {
					pi.x += c.x;
					pi.y += c.y;
					pj.x -= c.x;
					pj.y -= c.y;
					// TODO: Remove the velocities?
				}
			}

			for (int j = 0; j < walls.size(); ++j) {
				Wall wj = walls.get(j);
				Contact c = circleRectangleIntersection(pi.x, pi.y, 12, wj.x, wj.y, wj.w, wj.h);
				if (c != null) {
					pi.x += c.x;
					pi.y += c.y;
				}
			}
		}
	}

	public void sendInitial(AJAXConnection connection) {
		for (Wall wall : walls) connection.sendWall(wall);
		for (Player enemy : enemies) connection.sendEnemy(enemyIDs.get(enemy), enemy);
	}

	public void sendUpdate(AJAXConnection connection) {
		for (Player enemy : enemies) connection.sendEnemy(enemyIDs.get(enemy), enemy);
	}

	private void setAIInputs(Player enemy) {
		Player nearest = null;
		double distance2 = Double.MAX_VALUE;
		for (Player player : players) {
			double dx = player.x - enemy.x;
			double dy = player.y - enemy.y;
			double d2 = (dx * dx) + (dy * dy);
			if (d2 < distance2) {
				nearest = player;
				distance2 = d2;
			}
		}

		if (nearest == null) return;

		double dx = nearest.x - enemy.x;
		double dy = nearest.y - enemy.y;
		double d = Math.sqrt(dx * dx + dy * dy);

		enemy.setInputs(dx / d, dy / d);
	}

	private static Contact circleCircleIntersection(double x0, double y0, double r0, double x1, double y1, double r1) {
		double dx = x0 - x1;
		double dy = y0 - y1;
		double d = Math.sqrt(dx * dx + dy * dy);
		if (d < (r0 + r1)) {
			return new Contact((dx / d) * ((r0 + r1) - d), (dy / d) * ((r0 + r1) - d));
		} else {
			return null;
		}
	}

	// FIXME: This actually is a squareRectangleIntersection.
	private static Contact circleRectangleIntersection(double x0, double y0, double r0, double x1, double y1, double w1, double h1) {
		double dx = Math.abs(x0 - x1);
		double dy = Math.abs(y0 - y1);
		double d2 = (dx - w1 / 2) * (dx - w1 / 2) + (dy - h1 / 2) * (dy - h1 / 2);

		if (dx > (w1 / 2 + r0)) return null;
		if (dy > (h1 / 2 + r0)) return null;
		//if (d2 > r0 * r0) return null;

		double u = (y1 - h1 / 2) - (y0 + r0);
		double r = (x1 + w1 / 2) - (x0 - r0);
		double d = (y1 + h1 / 2) - (y0 - r0);
		double l = (x1 - w1 / 2) - (x0 + r0);

		double x = Math.abs(l) < r ? l : r;
		double y = Math.abs(u) < d ? u : d;
		if (Math.abs(x) < Math.abs(y)) return new Contact(x, 0);
		else return new Contact(0, y);
	}
}
