package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.relogo.Utility;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Doctor extends Human {
	private static final int SPEED = 1;
	private List<NdPoint> doorPoints;
	private boolean nearDoor;
	private int stepsTakenSinceDoor;
	private boolean shouldGoBackToDoor;
	
	public Doctor(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(15);
		this.setSpeed(SPEED);
		this.doorPoints = new ArrayList<>();
		this.nearDoor = false;
		this.stepsTakenSinceDoor = 0;
		this.shouldGoBackToDoor = false;
	}
	
	public void addDoor(NdPoint doorPoint) {
		this.doorPoints.add(doorPoint);
	}
	
	public void setShouldGoBackToDoor(boolean shouldGoBackToDoor) {
		this.shouldGoBackToDoor = shouldGoBackToDoor;
	}
	
	@ScheduledMethod(start = 1, interval = SPEED)
	public void run() {
		if (!isDead()) {
			if (nearDoor || shouldGoBackToDoor) {
				findPatients();
			} else {
				moveTowardsDoor();
			}
		}
	}
	
	private void findPatients() {
		GridPoint pt = this.getGrid().getLocation(this);
		
		GridPoint nextLocation = findNextLocation();
		
		stepsTakenSinceDoor++;
		
		if (stepsTakenSinceDoor > 10) {
			nearDoor = false;
		}
	}
	
	private void moveTowardsDoor() {
		GridPoint pt = this.getGrid().getLocation(this);
		
		double closestDoorDistance = Double.POSITIVE_INFINITY;
		NdPoint closestDoor = null;
		for (NdPoint doorPoint : doorPoints) {
			double distance = Math.sqrt(Math.pow(doorPoint.getX() - pt.getX(), 2)
					+ Math.pow(doorPoint.getY() - pt.getY(), 2));
			if (distance < closestDoorDistance) {
				closestDoor = doorPoint;
				closestDoorDistance = distance;
			}
		}
		GridPoint closestDoorPoint = Utility.ndPointToGridPoint(closestDoor);
		
		if (closestDoorDistance < 5) {
			//nearDoor = true;
		}
		
		if (closestDoorPoint != null) {
			moveTowards(closestDoorPoint);
		} else {
			this.kill();
		}
	}
	
	private GridPoint findNextLocation() {
		
		
		return null;
	}
}
