package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bitspls.evacuation.Door;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.GasParticle;
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
	private double basePanic;
	private double panic;
	private double worstCase;
	private double patientPanicWeight;
	private double gasPanicWeight;
	
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
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid, double patientPanicWeight, double gasPanicWeight, double meanPanic, double stdPanic, Random random) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.panic = getStartingPanic(meanPanic, stdPanic, random);
		this.setBasePanic(panic);
		this.setPanic(panic);
		this.setWorstCase(calculateWorstCaseScenario());
		this.setPatientPanicWeight(patientPanicWeight);
		this.setGasPanicWeight(gasPanicWeight);
		this.movementMode = PatientMode.AVOID_GAS;
		this.doctorToFollow = null;
		this.door = null;
		this.exited = false;
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
				if(targetDoctor != null && shouldFollowDoctorAgent(targetDoctor)) {
					this.doctorToFollow = targetDoctor;
					this.doctorToFollow.startFollowing();
					this.movementMode = PatientMode.FOLLOW_DOCTOR;
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
		double probabilityOfFollowingDoctor = 0.4*doctor.getCharisma() + 0.6*(1 - getPanic());
		
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
		double gasFactor = calculateGasParticleFactor();
		double patientFactor = calculateGasParticleFactor();
		double panic = this.basePanic + (this.patientPanicWeight * patientFactor) + (this.gasPanicWeight * gasFactor) ;
		return panic;
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
	 * Query all cells with gas particles present in the patient's radius of knowledge
	 * @return List of all cells containing gas inside the neighborhood
	 */
	private List<GridCell<GasParticle>> findGasAgentsInRadiusOfKnowledge() {
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridPoint location = getGrid().getLocation(this);

		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(getGrid(), location, GasParticle.class, radiusOfKnowledge, radiusOfKnowledge);
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
		
		List<GridCell<GasParticle>> gasAgentLocation = new ArrayList<GridCell<GasParticle>>();
		
		for (GridCell<GasParticle> cell : gridCells) {
			if(cell.size() > 0) {
				gasAgentLocation.add(cell);
			}
		}

		return gasAgentLocation;
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
	
	/**
	 * Calculate portion of panic level resulting from panicked patients near patient
	 * @return patient panic factor for panic level
	 */
	private double calculatePatientsPanicFactor() {
		List<Patient> patients = findPatientAgentsInRadiusOfKnowledge();	
		double worstCase = getWorstCase();
		double patientFactor = calculateSurroundingPatientsPanicFactor(patients);
		if (worstCase < patientFactor) {
			return patientFactor/worstCase;
		}
		else {
			return 1;
		}
	}
	
	/**
	 * Calculate portion of panic level resulting from panicked patients near patient
	 * @return patient panic factor for panic level
	 */
	private double calculateSurroundingPatientsPanicFactor(List<Patient> patients) {
		GridPoint currentLocation = getGrid().getLocation(this);
		double totalPatientFactor = 0.0;
		
		for(Patient patient: patients) {
			GridPoint pt = getGrid().getLocation(patient);
			double distance = getGrid().getDistance(currentLocation, pt);
			if (distance != 0) {
				totalPatientFactor += patient.getPanic()/distance;
			}
		}
		
		return totalPatientFactor;
	}
	
	/**
	 * Calculate the worst panic level possible
	 * @return The worst panic level
	 */
	public double calculateWorstCaseScenario() {
		GridDimensions dim = getGrid().getDimensions();
		GridPoint center = new GridPoint(dim.getWidth() / 2, dim.getHeight() / 2);
		
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridCellNgh<Patient> nghCreator = new GridCellNgh<Patient>(getGrid(), center, Patient.class,
				radiusOfKnowledge, radiusOfKnowledge);
		List<GridCell<Patient>> gridCells = nghCreator.getNeighborhood(false);

		double total = 0.0;
		for (GridCell<Patient> cell : gridCells) {
			GridPoint currentPt = cell.getPoint();
			double distance = getGrid().getDistance(center, currentPt);
			if (distance != 0) {
				total += 1/distance;
			}
		}
		return total/3;
	}
	
	/**
	 * Calculate portion of panic level resulting from gas near patient
	 * @return gas factor for panic level
	 */
	private double calculateGasParticleFactor() {
		List<GridCell<GasParticle>> surroundingGas = findGasAgentsInRadiusOfKnowledge();
		double surroundingFactor = calculateSurroundingGasParticleFactor(surroundingGas);
		if(surroundingFactor > worstCase) {
			return 1;
		} else {
			return calculateSurroundingGasParticleFactor(surroundingGas)/worstCase;
		}
	}
	
	/**
	 * Calculate portion of panic level resulting from gas near patient
	 * @return gas factor for panic level
	 */
	private double calculateSurroundingGasParticleFactor(List<GridCell<GasParticle>> gasAgents) {
		GridPoint location = getGrid().getLocation(this);
		double totalGasFactor = 0.0;
		
		for(GridCell<GasParticle> gas: gasAgents) {
			GridPoint pt = gas.getPoint();
			double distance = getGrid().getDistance(location, pt);
			totalGasFactor += 1/distance;
		}
		
		return totalGasFactor;
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
	
	public double getWorstCase() {
		return this.worstCase;
	}
	
	public void setWorstCase(double worstCase) {
		this.worstCase = worstCase;
	}
	
	public void setPatientPanicWeight(double weight) {
		this.patientPanicWeight = weight;
	}
	
	public void setGasPanicWeight(double weight) {
		this.gasPanicWeight = weight;
	}
	
	public void setBasePanic(double panic) {
		this.basePanic = panic;
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
