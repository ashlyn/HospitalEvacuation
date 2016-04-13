package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.relogo.Utility;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Doctor extends Human {
	private DoctorMode doctorMode;
	private static final int SPEED = 1;
	private List<NdPoint> doorPoints;
	private int followers;
	private double charisma;
	private int stepsTakenAwayFromDoor;
	
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
	}
	
	public void addDoor(NdPoint doorPoint) {
		this.doorPoints.add(doorPoint);
	}
	
	@ScheduledMethod(start = 1, interval = SPEED)
	public void run() {
		if (!isDead()) {
			exchangeInformationWithDoctors();
			if (doctorMode == DoctorMode.PATIENT_SEEK) {
				findPatients();
			} else {
				moveTowardsDoor();
			}
		}
	}
	
	private void findPatients() {
		if (stepsTakenAwayFromDoor < 30) {
			moveAwayFromDoor();
		} else {
			moveRandomly();
		}
	}
	
	private void exchangeInformationWithDoctors() {
		List<Doctor> doctorsInRadius = super.findDoctorsInRadius();
		for(Doctor doc : doctorsInRadius) {
			for(NdPoint door : doc.doorPoints) {
				if (!this.doorPoints.contains(door)) {
					this.doorPoints.add(door);
				}
			}
		}
	}
	
	private void moveRandomly() {
		GridPoint pt = this.getGrid().getLocation(this);
		GridPoint pointToMoveTo = super.findLeastGasPoint(pt);
		
		super.moveTowards(pointToMoveTo);
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
	
	private GridPoint findNextLocation() {
		
		
		return null;
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