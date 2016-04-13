package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Doctor extends Human {
	private static final int SPEED = 1;
	private List<NdPoint> doorPoints;
	private int followers;
	private double charisma;
	
	public Doctor(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(15);
		this.setSpeed(SPEED);
		this.doorPoints = new ArrayList<>();
		this.followers = 0;
	}
	
	public void addDoor(NdPoint doorPoint) {
		this.doorPoints.add(doorPoint);
	}
	
	@ScheduledMethod(start = 1, interval = SPEED)
	public void run() {
		if (!isDead()) {
			GridPoint pt = this.getGrid().getLocation(this);
			
			GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
			List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			GridPoint pointWithLeastGas = null;
			for (GridCell<GasParticle> cell : gridCells) {
				if (cell.size() == 0) {
					pointWithLeastGas = cell.getPoint();
				}
			}
			//System.out.println("Doctor: " + pt.getX() + " " + pt.getY());
			
			double closestDoorDistance = Double.POSITIVE_INFINITY;
			NdPoint closestDoor = null;
			for (NdPoint doorPoint : doorPoints) {
				double distance = Math.sqrt(Math.pow(doorPoint.getX() - pt.getX(), 2)
						+ Math.pow(doorPoint.getY() - pt.getY(), 2));
				if (distance < closestDoorDistance) {
					closestDoor = doorPoint;
					closestDoorDistance = distance;
					//System.out.println("closer " + distance);
				}
			}
			
			if (pointWithLeastGas != null) {
				moveTowards(pointWithLeastGas);
			} else {
				this.kill();
			}
		}
	}
	
	public void startFollowing() {
		this.followers++;
	}
	
	public void stopFollowing() {
		this.followers--;
	}
	
	public int getFollowers() {
		return this.followers;
	}
	
	public double getCharisma() {
		return this.charisma;
	}
	
	public void setCharisma(double charisma) {
		this.charisma = charisma;
	}
}
