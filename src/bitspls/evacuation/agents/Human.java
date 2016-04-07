package bitspls.evacuation.agents;

import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public abstract class Human {
	private boolean dead;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int radiusOfKnowledge;
	private int speed;
	
	protected void moveTowards(GridPoint pt) {
		if (pt != null && !pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 2, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
		}
	}
	
	protected boolean isDead() {
		return this.dead;
	}
	
	protected void setDead(boolean dead) {
		this.dead = dead;
	}
	
	protected void kill() {
		this.dead = true;
	}
	
	protected ContinuousSpace<Object> getSpace() {
		return this.space;
	}
	
	protected void setSpace(ContinuousSpace<Object> space) {
		this.space = space;
	}
	
	protected Grid<Object> getGrid() {
		return this.grid;
	}
	
	protected void setGrid(Grid<Object> grid) {
		this.grid = grid;
	}
	
	protected int getRadiusOfKnowledge() {
		return this.radiusOfKnowledge;
	}
	
	protected void setRadiusOfKnowledge(int r) {
		this.radiusOfKnowledge = r;
	}
	
	protected int getSpeed() {
		return this.speed;
	}
	
	protected void setSpeed(int speed) {
		this.speed = speed;
	}
}
