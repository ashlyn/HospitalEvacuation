package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bitspls.evacuation.Door;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.Human;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridDimensions;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

/**
 * Class to model the Patient agent
 * Patients move randomly until they find a doctor or a door
 * Upon finding a doctor, they follow them to the door and exit
 * If they find a door, they move towards the door and exit
 * 
 * Patients extend Human, which has some mechanics for all
 * human agents in regards to moving & avoiding gas
 * @author Bits Please
 *
 */
public class Patient extends Human {
	private PatientMode movementMode;
	private Doctor doctorToFollow;
	private Door door;
	private boolean exited;
	private double panic;
	private double patientPanicWeight;
	private List<Doctor> blackListedDoctors;
	
	/**
	 * Constructor for Patient agent
	 * @param space Continuous space the patient is located in
	 * @param grid Grid the patient is located in
	 * @param patientPanicWeight Weighting factor for the effects of other patients' panic levels
	 * @param gasPanicWeight Weighting factor for the effects of gas on panic
	 * @param meanPanic Mean panic level for all patients
	 * @param stdPanic Standard deviation of panic level for all patients
	 * @param random RNG to set this instance's starting panic level
	 */
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid, double patientPanicWeight, double meanPanic, double stdPanic, Random random) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.panic = getStartingPanic(meanPanic, stdPanic, random);
		this.setPanic(panic);
		this.setPatientPanicWeight(patientPanicWeight);
		this.movementMode = PatientMode.AVOID_GAS;
		this.doctorToFollow = null;
		this.door = null;
		this.exited = false;
		this.blackListedDoctors = new ArrayList<Doctor>();
	}
	
	private double getStartingPanic(double meanPanic, double stdPanic, Random random) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		String panicDistribution = params.getString("dist_panic");
		if (panicDistribution.toUpperCase().equals("CONSTANT")) {
			return meanPanic;
		}
		else if (panicDistribution.toUpperCase().equals("UNIFORM")) {
			return random.nextDouble();
		}
		else {
			return stdPanic * random.nextGaussian() + meanPanic;
		}
	}
	
	/**
	 * Move function that is scheduled for every tick to move
	 * the patient if it is not dead
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		if (!isDead() && !this.exited) {
			
			/*
			 * Panic level is re-evaluated based on the number and panic
			 * level of other patients around and the amount and closeness
			 * of gas to the patient
			 */			
			this.setPanic(this.calculateNewPanicLevel());

			/*
			 * Follow a doctor in the area if available
			 */
			if (this.doctorToFollow == null || this.door != null) {
				Doctor targetDoctor = findDoctorWithMaxCharisma();
				if (targetDoctor != null && !this.blackListedDoctors.contains(targetDoctor)) 
				{
					if(shouldFollowDoctorAgent(targetDoctor)) 
					{
						System.out.println("following doctor");
						this.doctorToFollow = targetDoctor;
						this.doctorToFollow.startFollowing();
						this.movementMode = PatientMode.FOLLOW_DOCTOR;
					}
					else 
					{
						this.blackListedDoctors.add(targetDoctor);
					}
				}
			}
			
			/*
			 * Look for doors in the area if the patient hasn't found one yet
			 */
			if (this.movementMode != PatientMode.APPROACH_DOOR) {
				Door closestDoor = findClosestDoor();
				if (closestDoor != null) {
					this.door = closestDoor;
					if (this.doctorToFollow != null) this.doctorToFollow.stopFollowing();
					this.movementMode = PatientMode.APPROACH_DOOR;
				}
			}
			determineNextAction(findNextPointToMoveTo());
		}
	}
	
	/**
	 * If there are no valid points to move to (the patient is surrounded by gas)
	 * the patient dies, otherwise they move towards that point
	 */
	private void determineNextAction(GridPoint pointToMoveTo) {
		if (pointToMoveTo != null) {
			moveTowards(pointToMoveTo);
		} else {
			this.kill();
		}
	}
	
	/**
	 * Find the next point to move towards based on the movement mode of the patient
	 * @return The best point to move towards
	 */
	private GridPoint findNextPointToMoveTo() {
		GridPoint currentLocation = this.getGrid().getLocation(this);
		GridPoint leastGasPoint = findLeastGasPoint(currentLocation);
		GridPoint pointToMoveTo = null;
		
		// Move towards the door if available
		if (this.movementMode == PatientMode.APPROACH_DOOR) {
			pointToMoveTo = this.getGrid().getLocation(this.door);
		} else if (this.movementMode == PatientMode.FOLLOW_DOCTOR && !this.doctorToFollow.isDead()) {  // follow the doctor if available
			pointToMoveTo = this.getGrid().getLocation(this.doctorToFollow);
		} else if (leastGasPoint != null) { // otherwise, just avoid gas
			pointToMoveTo = leastGasPoint;
		}
		
		return pointToMoveTo;
	}
	
	/**
	 * Moves the patient towards a point and if it is a door, causes the patient
	 * to wait by the door until it can exit
	 */
	protected void moveTowards(GridPoint pt) {
		super.moveTowards(pt);
		
		if (this.door != null) {
			GridPoint currentPt = this.getGrid().getLocation(this);
			GridCellNgh<Door> doorNghCreator = new GridCellNgh<Door>(this.getGrid(), currentPt, Door.class, 0, 0);
			List<GridCell<Door>> doorGridCells = doorNghCreator.getNeighborhood(true);
			GridPoint doorPt = this.getGrid().getLocation(this.door);
			if (doorGridCells.contains(doorPt)) {
				this.exited = true;
				this.doctorToFollow.stopFollowing();
				this.doctorToFollow = null;
				this.door = null;
			}
		}
	}

	/**
	 * Determines if the patient should follow a given doctor
	 * based on the doctor's charisma and patient's panic
	 * @param doctor Doctor patient may follow
	 * @return a boolean representing whether the patient should follow the doctor
	 */
	public boolean shouldFollowDoctorAgent(Doctor doctor) {
		double panicWeight = this.getPatientPanicWeight();
		double probabilityOfFollowingDoctor = (1 - panicWeight)*doctor.getCharisma() + panicWeight*(1 - this.getPanic());
		return randomFollowGenerator(probabilityOfFollowingDoctor);
	}
	
	/**
	 * If a random number passes a threshold, return true
	 * @param probabilityTrue The threshold that the RNG must pass
	 * @return whether the RNG > probabilityTrue
	 */
	public boolean randomFollowGenerator(double probabilityTrue)
	{
	    return Math.random() >= 1.0 - probabilityTrue;
	}
	
	/**
	 * Calculate a new panic level based on the gas around a patient
	 * and the magnitude of patient panic around a patient
	 * @return new panic level
	 */
	public double calculateNewPanicLevel() {
		List<Patient> patients = this.findPatientAgentsInRadiusOfKnowledge();
		double totalPatientPanic = 0.0;
		
		for(Patient patient: patients) {
			if (patient.getPanic() >= 0) {
				totalPatientPanic += patient.getPanic();
			}
		}
		return totalPatientPanic/patients.size();
	}
	
	/**
	 * Search for the closest door inside of the patient's radius of knowledge
	 * If there is no such door, return null
	 * @return The closest door to the patient within it's radius of knowledge
	 */
	private Door findClosestDoor() {
		GridPoint currentLocation = this.getGrid().getLocation(this);
		GridCellNgh<Door> doorNghCreator = new GridCellNgh<Door>(this.getGrid(), currentLocation, Door.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
		List<GridCell<Door>> doorGridCells = doorNghCreator.getNeighborhood(true);
		
		double minimumDistance = Double.POSITIVE_INFINITY;
		Door closestDoor = null;
		
		for (GridCell<Door> cell : doorGridCells) {
			if (cell.size() > 0) {
				for (Door door : cell.items()) {
					double distance = getGrid().getDistance(currentLocation, getGrid().getLocation(door));	
					if (distance < minimumDistance) {
						closestDoor = door;
					}
				}
			}
		}
		
		return closestDoor;
	}
	
	/**
	 * Finds the best doctor to follow (the doctor with highest charisma)
	 * inside of the patient's radius of knowledge
	 * @return The best doctor to follow inside a neighborhood
	 */
	private Doctor findDoctorWithMaxCharisma() {
		GridPoint currentLocation = this.getGrid().getLocation(this);
		GridCellNgh<Doctor> doctorNghCreator = new GridCellNgh<Doctor>(this.getGrid(), currentLocation, Doctor.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
		List<GridCell<Doctor>> doctorGridCells = doctorNghCreator.getNeighborhood(true);
		SimUtilities.shuffle(doctorGridCells, RandomHelper.getUniform());
		
		double maxCharisma = 0.0;
		Doctor maxDoctor = null;
		for (GridCell<Doctor> cell : doctorGridCells) {
			if (cell.size() > 0) {
				for (Doctor doc : cell.items()) {
					if(doc.getCharisma() > maxCharisma) {
						maxDoctor = doc;
						maxCharisma = doc.getCharisma();
					}
				}
			}
		}
		
		return maxDoctor;
	}
	
	/**
	 * Query all cells with patients present in the patient's radius of knowledge
	 * @return List of all cells containing patients inside the neighborhood
	 */
	private List<Patient> findPatientAgentsInRadiusOfKnowledge() {
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridPoint location = getGrid().getLocation(this);
		
		GridCellNgh<Patient> nghCreator = new GridCellNgh<Patient>(getGrid(), location, Patient.class, radiusOfKnowledge, radiusOfKnowledge);
		List<GridCell<Patient>> gridCells = nghCreator.getNeighborhood(true);

		List<Patient> patientsInRadius = new ArrayList<Patient>();
		
		for (GridCell<Patient> cell : gridCells) {
			for (Object obj: getGrid().getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY())) {
				if (obj instanceof Patient) {
					patientsInRadius.add((Patient) obj);
				}
			}
		}
		return patientsInRadius;
	}
	
	/*
	 * Getters and Setters
	 */
	public double getPanic() {
		return this.panic;
	}
	
	public void setPanic(double panic) {
		this.panic = panic;
	}

	public double getPatientPanicWeight() {
		return this.patientPanicWeight;
	}
	
	public void setPatientPanicWeight(double weight) {
		this.patientPanicWeight = weight;
	}
	
	/**
	 * Kill a patient by removing it from the context
	 */
	public void kill() {
    	super.kill();

    	/*
    	 *  Note: We are not giving the agent global knowledge of the other agents in the system
    	 *  These counts are only to stop the simulation when no human agents remain
    	 */
    	Context<Object> context = ContextUtils.getContext(this);
    	int humanCount = context.getObjects(Doctor.class).size() + context.getObjects(Patient.class).size();
    	
    	if (humanCount > 1) {
	    	GridPoint pt = this.getGrid().getLocation(this);
	    	NdPoint spacePt = new NdPoint(pt.getX(), pt.getY());
	
			DeadPatient deadPatient = new DeadPatient();
			context.add(deadPatient);
			this.getSpace().moveTo(deadPatient, spacePt.getX(), spacePt.getY());
			this.getGrid().moveTo(deadPatient, pt.getX(), pt.getY());
			
			context.remove(this);
    	} else {
    		RunEnvironment.getInstance().endRun();
    	}
    }
	
	/**
	 * Enum to represent the patient's movement mode
	 * @author Bits Please
	 *
	 */
	enum PatientMode {
		AVOID_GAS,		// Move randomly and avoid gas particles
		FOLLOW_DOCTOR,  // Follow a doctor to a door
		APPROACH_DOOR	// Approach a door and exit
	}
}
