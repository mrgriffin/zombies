public class Player {
	// TODO: Make the instance variables private.
	public String name;
	public double x, y;
	public double vx, vy;

	public Player(String name, double x, double y, double vx, double vy) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
	}

	public void update(double dt) {
		x += vx * dt;
		y += vy * dt;
	}
}
