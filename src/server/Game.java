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

	// TODO: Use the index in the array as the ID and do not remove from the list.
	private List<Player> enemies = new ArrayList<>();
	private Map<Player, Integer> enemyIDs = new HashMap<>();
	private int enemyID = 0;

	private List<Shot> shots = new ArrayList<>();
	private Map<Shot, Integer> shotIDs = new HashMap<>();
	private int shotID = 0;

	private int enemyWave;
	private long nextEnemyWave;

	public Game() {
		enemyWave = 0;
		nextEnemyWave = System.currentTimeMillis();
	}

	// TODO: Do not abuse contact as a Point2D.
	private Contact findFreePoint(double r) {
		while (true) {
			// NOTE: 0..460 + 20 because the intersection testing doesn't seem to work.
			double x = Math.random() * 460 + 20;
			double y = Math.random() * 460 + 20;
			for (Player p : players) if (circleCircleIntersection(p.x, p.y, 12, x, y, r) != null) continue;
			for (Player e : enemies) if (circleCircleIntersection(e.x, e.y, 12, x, y, r) != null) continue;
			for (Shot s : shots) if (circleCircleIntersection(s.x, s.y, 4, x, y, r) != null) continue;
			for (Wall w : walls) if (circleRectangleIntersection(x, y, r, w.x, w.y, w.w, w.h) != null) continue;
			return new Contact(x, y);
		}
	}

	private void generateEnemyWave() {
		enemyWave++;
		nextEnemyWave = System.currentTimeMillis() + (long)(10.0 / (4 + enemyWave) * 30000);
		int enemyCount = (1 + enemyWave / 2) * 3;
		for (int i = 0; i < enemyCount; ++i) {
			int type = (int)(Math.floor(Math.random() * 5)); // 0: Small, 1-3: Regular, 4: Large.
			int size = 0, speed = 0, health = 0;
			switch (type) {
			case 0:
				size = 6; speed = 65; health = 10; break;
			case 1: case 2: case 3:
				size = 12; speed = 50; health = 25; break;
			case 4:
				size = 24; speed = 35; health = 75; break;
			}

			Contact c = findFreePoint(24 + size);
			addEnemy(new Player(null, c.x, c.y, speed + 2 * enemyWave, health, size));
		}
	}

	public void addPlayer(Player player) {
		players.add(player);
	}

	private void addEnemy(Player enemy) {
		enemies.add(enemy);
		enemyIDs.put(enemy, enemyID++);
	}

	public void addWall(Wall wall) {
		walls.add(wall);
	}

	public void update(double dt) {
		if (System.currentTimeMillis() >= nextEnemyWave) generateEnemyWave();

		for (Player player : players) player.update(dt);
		for (Player enemy : enemies) { setAIInputs(enemy); enemy.update(dt); }
		for (Shot shot : shots) shot.update(dt);

		// Player-{Player,Enemy,Wall} collisions.
		for (int i = 0; i < players.size(); ++i) {
			Player pi = players.get(i);

			if (pi.health <= 0) continue;

			// TODO: Move this up to the update section.
			if (pi.isRangedAttacking()) {
				// TODO: Refactor this and isRangedAttacking into rangedAttack -> Shot.
				double x = pi.x + pi.ox * (pi.size + 4);
				double y = pi.y + pi.oy * (pi.size + 4);
				Shot shot = new Shot(pi, x, y, pi.ox * 150, pi.oy * 150);
				shots.add(shot);
				shotIDs.put(shot, shotID++);
			}

			for (int j = i + 1; j < players.size(); ++j) {
				Player pj = players.get(j);
				if (pj.health <= 0) continue;
				Contact c = circleCircleIntersection(pi.x, pi.y, pi.size, pj.x, pj.y, pj.size);
				if (c != null) {
					// TODO: Prevent pushing?
					pi.x += c.x;
					pi.y += c.y;
					pj.x -= c.x;
					pj.y -= c.y;
					pi.pushed = pj.pushed = true;
				}
			}

			for (int j = 0; j < enemies.size(); ++j) {
				Player ej = enemies.get(j);
				Contact c = circleCircleIntersection(pi.x, pi.y, pi.size, ej.x, ej.y, ej.size);
				if (c != null) {
					pi.x += c.x;
					pi.y += c.y;
					ej.x -= c.x;
					ej.y -= c.y;
					ej.meleeAttack(pi);
					pi.pushed = ej.pushed = true;
				}
			}

			for (int j = 0; j < walls.size(); ++j) {
				Wall wj = walls.get(j);
				Contact c = circleRectangleIntersection(pi.x, pi.y, pi.size, wj.x, wj.y, wj.w, wj.h);
				if (c != null) {
					pi.x += c.x;
					pi.y += c.y;
					pi.pushed = true;
				}
			}
		}

		// Shot-{Enemy,Wall} collisions.
		for (int i = 0; i < shots.size(); ++i) {
			Shot si = shots.get(i);

			for (int j = 0; j < enemies.size(); ++j) {
				Player ej = enemies.get(j);
				Contact c = circleCircleIntersection(si.x, si.y, 4, ej.x, ej.y, ej.size);
				if (c != null) {
					si.dead = true;
					ej.health -= 25;
					break;
				}
			}

			for (int j = 0; j < walls.size(); ++j) {
				Wall wj = walls.get(j);
				Contact c = circleRectangleIntersection(si.x, si.y, 4, wj.x, wj.y, wj.w, wj.h);
				if (c != null) {
					si.dead = true;
					break;
				}
			}
		}

		// Enemy-{Enemy,Wall} collisions.
		for (int i = 0; i < enemies.size(); ++i) {
			Player ei = enemies.get(i);

			for (int j = i + 1; j < enemies.size(); ++j) {
				Player ej = enemies.get(j);
				Contact c = circleCircleIntersection(ei.x, ei.y, ei.size, ej.x, ej.y, ej.size);
				if (c != null) {
					// TODO: Prevent pushing?
					ei.x += c.x;
					ei.y += c.y;
					ej.x -= c.x;
					ej.y -= c.y;
					ei.pushed = ej.pushed = true;
				}
			}

			for (int j = 0; j < walls.size(); ++j) {
				Wall wj = walls.get(j);
				Contact c = circleRectangleIntersection(ei.x, ei.y, ei.size, wj.x, wj.y, wj.w, wj.h);
				if (c != null) {
					ei.x += c.x;
					ei.y += c.y;
				}
			}
		}

		if (!enemies.isEmpty()) {
			boolean aliveEnemy = false;
			for (Player enemy : enemies) if (enemy.health > 0) aliveEnemy = true;
			if (!aliveEnemy) nextEnemyWave = System.currentTimeMillis() + 500;
		}
	}

	public void sendInitial(AJAXConnection connection) {
		for (Wall wall : walls) connection.sendWall(wall);
		for (Player enemy : enemies) connection.sendEnemy(enemyIDs.get(enemy), enemy);
	}

	public void sendUpdate(AJAXConnection connection) {
		for (int i = enemies.size() - 1; i >= 0; --i) {
			Player enemy = enemies.get(i);
			if (enemy.health > 0) {
				connection.sendEnemy(enemyIDs.get(enemy), enemy);
			} else {
				connection.sendEnemyDeath(enemyIDs.get(enemy));
				enemies.remove(i);
			}
		}

		for (int i = shots.size() - 1; i >= 0; --i) {
			Shot shot = shots.get(i);
			if (!shot.dead) {
				connection.sendShot(shotIDs.get(shot), shot);
			} else {
				connection.sendShotDeath(shotIDs.get(shot));
				shots.remove(i);
			}
		}
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

		enemy.setInputs(dx / d, dy / d, false);
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
