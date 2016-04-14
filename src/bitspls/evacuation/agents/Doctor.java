package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bitspls.evacuation.DoctorDoorPoint;
import javafx.util.Pair;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.relogo.Utility;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

/**
 * Class to model the Doctor agent
 * Doctors initially attempt to avoid gas and when
 * they encounter patients, they lead the patients
 * to one of the doors they have knowledge of, then
 * continue exploring the space
 * 
 * Doctors have the ability to learn about other doors
 * in the system from encountering them or by communicating
 * with other doctors who share their knowledge
 * 
 * Doctors extend Human, which has some mechanics for all
 * human agents in regards to moving & avoiding gas
 * @author Bits Please
 *
 */
public class Doctor extends Human {
	private DoctorMode doctorMode;
	private static final int SPEED = 1;
	private List<DoctorDoorPoint> doorPoints;
	private int followers;
	private double charisma;
	private int stepsTakenAwayFromDoor;
	
	/**
	 * Constructor for Doctor agent
	 * @param space The continuous space in which the agent is located
	 * @param grid The grid in which the agent is located
	 * @param meanCharisma The mean charisma level for all doctors
	 * @param stdCharisma The standard deviation of charisma for all doctors
	 * @param random An RNG to set this instance's charisma
	 */
	public Doctor(ContinuousSpace<Object> space, Grid<Object> grid, double meanCharisma, double stdCharisma, Random random) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(15);
		this.setSpeed(SPEED);
		this.doorPoints = new ArrayList<>();
		this.followers = 0;
		this.charisma = stdCharisma * random.nextGaussian() + meanCharisma;
		this.doctorMode = DoctorMode.DOOR_SEEK;
		this.stepsTakenAwayFromDoor = 0;
	}
	
	/**
	 * Adds a door to the doctor's knowledge
	 * @param doorPoint Point where the door is located
	 */
	public void addDoor(NdPoint doorPoint) {
		double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		DoctorDoorPoint ddp = new DoctorDoorPoint(doorPoint, false, false, ticks);
		this.doorPoints.add(ddp);
	}
	
	/**
	 * Scheduled method to move a doctor if they are still alive
	 */
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
	
	/**
	 * Moves a doctor away from a door if they have just
	 * dropped off patients and moves them randomly to 
	 * explore for more patients if not actively leading
	 */
	private void findPatients() {
		if (stepsTakenAwayFromDoor < 30) {
			moveAwayFromDoor();
		} else {
			moveRandomly();
		}
	}
	
	/**
	 * Exchanges information of doors with other doctors
	 * in this doctor's neighborhood
	 */
	private void exchangeInformationWithDoctors() {
		List<Doctor> doctorsInRadius = super.findDoctorsInRadius();
		for(Doctor doc : doctorsInRadius) {
			for(DoctorDoorPoint door : doc.doorPoints) {
				if (!this.doorPoints.contains(door)) {
					this.doorPoints.add(door);
				}
				for (DoctorDoorPoint myDoor : this.doorPoints) {
					if (!myDoor.isOvercrowded()) {
						myDoor.setOvercrowded(door.isOvercrowded() && door.getLastVisitedTime() >= myDoor.getLastVisitedTime());
					}
					if (!myDoor.isBlocked()) {
						myDoor.setBlocked(door.isBlocked());
					}
				}
			}
		}
	}
	
	/**
	 * Move towards a random point with no gas (simple gas avoidance)
	 */
	private void moveRandomly() {
		GridPoint pt = this.getGrid().getLocation(this);
		GridPoint pointToMoveTo = super.findLeastGasPoint(pt);
		
		super.moveTowards(pointToMoveTo);
	}
	
	/**
	 * Moves a doctor away from the door in the direction they 
	 * came from if recently dropped off patients
	 */
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
	
	/**
	 * Move towards a the closest available (unblocked and not overcrowded)
	 * door if leading patients
	 */
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
	
	/**
	 * Finds the closest available door to this doctor
	 * @return A key-value pair of the distance and the grid point corresponding to the door
	 */
	private Pair<Double, GridPoint> findClosestDoor() {
		GridPoint pt = this.getGrid().getLocation(this);
		
		double closestDoorDistance = Double.POSITIVE_INFINITY;
		NdPoint closestDoor = null;
		for (DoctorDoorPoint doorPoint : doorPoints) {
			double distance = this.getSpace().getDistance(new NdPoint(pt.getX(), pt.getY()), doorPoint.getPoint());
			if (distance < closestDoorDistance && !doorPoint.isBlocked() && !doorPoint.isOvercrowded()) {
				closestDoor = doorPoint.getPoint();
				closestDoorDistance = distance;
			}
		}
		GridPoint closestDoorPoint = Utility.ndPointToGridPoint(closestDoor);
		
		return new Pair<Double, GridPoint>(closestDoorDistance, closestDoorPoint);
	}
	
	private GridPoint findNextLocation() {
		
		
		return null;
	}
	
	/**
	 * Begin following this doctor
	 * Increment the followers counter and send the doctor towards a door
	 */
	public void startFollowing() {
		this.followers++;
		doctorMode = DoctorMode.DOOR_SEEK;
	}
	
	/**
	 * Stop following a doctor. If that was the last patient following,
	 * send the doctor to find more patients
	 */
	public void stopFollowing() {
		this.followers--;
		if (followers <= 0) {
			doctorMode = DoctorMode.PATIENT_SEEK;
		}
	}
	
	/*
	 * Getters and setters for charisma, followers
	 */
	public int getFollowers() {
		return this.followers;
	}

	public double getCharisma() {
		return this.charisma;
	}
	
	public void setCharisma(double charisma) {
		this.charisma = charisma;
	}
	
	/**
	 * Enum to represent the state of a doctor
	 * @author Bits Please
	 */
	public enum DoctorMode {
		DOOR_SEEK,  	// should move towards closest available door
		PATIENT_SEEK,	// should look for patients while avoiding gas
		ESCAPE			// move to door and exit
	}
}
