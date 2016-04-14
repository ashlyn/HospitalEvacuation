package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.relogo.Utility;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Doctor extends Human {
	private DoctorMode doctorMode;
	private static final int SPEED = 1;
	private List<NdPoint> doorPoints;
	private int followers;
	private double charisma;
	private int stepsTakenAwayFromDoor;
	private double previousAngle;
	
	public Doctor(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(15);
		this.setSpeed(SPEED);
		this.doorPoints = new ArrayList<>();
		this.followers = 0;
		this.charisma = .5;
		this.doctorMode = DoctorMode.DOOR_SEEK;
		this.stepsTakenAwayFromDoor = 0;
		this.previousAngle = Double.POSITIVE_INFINITY;
	}
	
	public void addDoor(NdPoint doorPoint) {
		this.doorPoints.add(doorPoint);
	}
	
	@ScheduledMethod(start = 1, interval = SPEED)
	public void run() {
		if (!isDead()) {
			if (doctorMode == DoctorMode.PATIENT_SEEK) {
				findPatients();
			} else {
				moveTowardsDoor();
			}
		}
	}
	
	private void findPatients() {
			moveRandomly();
	}

	private void moveRandomly() {
		GridPoint pt = this.getGrid().getLocation(this);
		
		/*double angleB = previousAngle;
		
		double angleA = Math.PI / 2;
		 
		double angleC = Math.PI - angleA - angleB;

		double sideBLength = Math.sin(angleB);
		double sideCLength = Math.sin(angleC);
		
		int newX = pt.getX();
		int newY = pt.getY();
		
		if (angleB >= 0 && angleB <= Math.PI / 8) {
			newX += sideCLength;
			newY += sideBLength;
		} else if (angleB > Math.PI / 8 && angleB <= Math.PI / 4) {
			newX += sideCLength;
			newY += sideBLength;
		} else if (angleB > Math.PI / 4 && angleB <= Math.PI * 3 / 4) {
			newX -= sideBLength;
			newY += sideCLength;
		} else if (angleB > Math.PI * 3 / 4 && angleB <= Math.PI) {
			newX -= sideBLength;
			newY += sideCLength;
		} else if (angleB > Math.PI && angleB <= Math.PI * 5 / 4) {
			newX -= sideCLength;
			newY -= sideBLength;
		} else if (angleB > Math.PI * 5 / 4 && angleB <= Math.PI * 3 / 2) {
			newX -= sideCLength;
			newY -= sideBLength;
		} else if (angleB > Math.PI * 3 / 2 && angleB <= Math.PI * 7 / 4) {
			newX += sideBLength;
			newY -= sideCLength;
		} else {
			newX += sideBLength;
			newY -= sideCLength;
		}
		
		System.out.println("x,y" + newX + " " + newY);
		*/

		float xRand = RandomHelper.getUniform().nextFloatFromTo(0, 1);
		int xShift = xRand <= .34 ? 0 : xRand <= .67 ? 1 : -1;
		
		float yRand = RandomHelper.getUniform().nextFloatFromTo(0, 1);
		int yShift = yRand <= .34 ? -1 : yRand <= .67 ? 0 : 1;
		
		GridPoint point = new GridPoint(pt.getX() + xShift, pt.getY() + yShift);
		
		System.out.println("old " + pt.getX() + " " + pt.getY() + " new " + point.getX() + " " + point.getY());
		
		super.moveTowards(point);
	}
	
	private void moveAwayFromDoor() {
		this.stepsTakenAwayFromDoor++;
		Pair<Double, GridPoint> distancePointPair = findClosestDoor();
		GridPoint doorLocation = distancePointPair.getValue();
		
		NdPoint myPoint = getSpace().getLocation(this);
		NdPoint otherPoint = new NdPoint(doorLocation.getX(), doorLocation.getY());
		double angleAwayFromDoor = SpatialMath.calcAngleFor2DMovement(getSpace(), myPoint, otherPoint);
		
		angleAwayFromDoor -= angleAwayFromDoor > Math.PI ? Math.PI : -Math.PI; 
		
		move(angleAwayFromDoor);
	}
	
	private void moveTowardsDoor() {
		Pair<Double, GridPoint> distancePointPair = findClosestDoor();
		
		double closestDoorDistance = distancePointPair.getKey();
		GridPoint closestDoorPoint = distancePointPair.getValue();
		
		if (closestDoorDistance < 3) {
			doctorMode = DoctorMode.PATIENT_SEEK;
			this.stepsTakenAwayFromDoor = 0;
		}
		
		if (closestDoorPoint != null) {
			moveTowards(closestDoorPoint);
		} else {
			this.kill();
		}
	}
	
	private Pair<Double, GridPoint> findClosestDoor() {
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
		
		return new Pair<Double, GridPoint>(closestDoorDistance, closestDoorPoint);
	}
	
	public void startFollowing() {
		this.followers++;
		doctorMode = DoctorMode.DOOR_SEEK;
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
	
	public enum DoctorMode {
		DOOR_SEEK,
		PATIENT_SEEK,
		ESCAPE
	}
}
