package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import bitspls.evacuation.Door;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.GasParticle;
import bitspls.evacuation.agents.Human;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Patient extends Human {
	private static final int SPEED = 2;

	private PatientMode movementMode;
	private Doctor doctorToFollow;
	private Door door;
	private boolean exited;
	private double panic;
	
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.setSpeed(SPEED);
		this.setPanic(0.5);
		this.movementMode = PatientMode.AVOID_GAS;
		this.doctorToFollow = null;
		this.door = null;
		this.exited = false;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		if (!isDead() && !this.exited) {
			this.setPanic(this.calculateNewPanicLevel());

			if (this.doctorToFollow == null || this.door != null) {
				Doctor targetDoctor = findDoctorWithMaxCharisma();
				if(shouldFollowDoctorAgent(targetDoctor)) {
					this.doctorToFollow = targetDoctor;
					this.doctorToFollow.startFollowing();
					this.movementMode = PatientMode.FOLLOW_DOCTOR;
				}
			}
			
			if (this.movementMode != PatientMode.APPROACH_DOOR) {
				Door closestDoor = findClosestDoor();
				if (closestDoor != null) {
					this.door = closestDoor;
					this.movementMode = PatientMode.APPROACH_DOOR;
				}
			}
			
			determineNextAction(findNextPointToMoveTo());
		}
	}
	
	private void determineNextAction(GridPoint pointToMoveTo) {
		if (pointToMoveTo != null) {
			moveTowards(pointToMoveTo);
		} else {
			this.kill();
		}
	}
	
	private GridPoint findNextPointToMoveTo() {
		GridPoint currentLocation = this.getGrid().getLocation(this);
		GridPoint leastGasPoint = findLeastGasPoint(currentLocation);
		GridPoint pointToMoveTo = null;
		
		if (this.movementMode == PatientMode.APPROACH_DOOR) {
			pointToMoveTo = this.getGrid().getLocation(this.door);
		} else if (this.movementMode == PatientMode.FOLLOW_DOCTOR && !this.doctorToFollow.isDead()) {
			pointToMoveTo = this.getGrid().getLocation(this.doctorToFollow);
		} else if (leastGasPoint != null) {
			pointToMoveTo = leastGasPoint;
		}
		
		return pointToMoveTo;
	}
	
	private GridPoint findLeastGasPoint(GridPoint pt) {
		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		GridPoint pointWithLeastGas = null;
		for (GridCell<GasParticle> cell : gridCells) {
			if (cell.size() == 0) {
				pointWithLeastGas = cell.getPoint();
			}
		}
		return pointWithLeastGas;
	}
	
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
				System.out.println("Patient exited");
			}
		}
	}

	public boolean shouldFollowDoctorAgent(Doctor doctor) {
		double probabilityOfFollowingDoctor = 0.4*doctor.getCharisma() + 0.6*(1 - getPanic());
		return randomFollowGenerator(probabilityOfFollowingDoctor);
	}
	
	public boolean randomFollowGenerator(double probabilityTrue)
	{
	    return Math.random() >= 1.0 - probabilityTrue;
	}
	
	public double calculateNewPanicLevel() {
		return (0.4 * getPanic()) + (0.3 * calculatePatientsPanicFactor()) + (0.3 * calculateGasParticleFactor() );
	}
	
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
	
	private List<GasParticle> findGasAgentsInRadiusOfKnowledge () {
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridPoint location = getGrid().getLocation(this);

		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(getGrid(), location, GasParticle.class, radiusOfKnowledge, radiusOfKnowledge);
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
		
		List<GasParticle> gasAgentsInRadius = new ArrayList<GasParticle>();
		
		for (GridCell<GasParticle> cell : gridCells) {
			for (Object obj: getGrid().getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY())) {
				if (obj instanceof GasParticle) {
					gasAgentsInRadius.add((GasParticle) obj);
				}
			}
		}
		
		return gasAgentsInRadius;
	}
	
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
	
	private double calculatePatientsPanicFactor() {
		List<Patient> patients = findPatientAgentsInRadiusOfKnowledge();
		return calculateSurroundingPatientsPanicFactor(patients)/calculateWorstCaseScenario();
	}
	
	private double calculateSurroundingPatientsPanicFactor(List<Patient> patients) {
		GridPoint location = getGrid().getLocation(this);
		double totalPatientFactor = 0.0;
		
		for(Patient patient: patients) {
			GridPoint pt = getGrid().getLocation(patient);
			double distance = getGrid().getDistance(location, pt);
			totalPatientFactor += patient.getPanic()/distance;
		}
		
		return totalPatientFactor;
	}
	
	private double calculateWorstCaseScenario() {	
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridPoint location = getGrid().getLocation(this);
		int xLocation = location.getX();
		int yLocation = location.getY();
		
		double total = 0.0;
		
		for(int i = xLocation - radiusOfKnowledge; i < xLocation + radiusOfKnowledge; i++) {
			for(int j = yLocation - radiusOfKnowledge; j < yLocation + radiusOfKnowledge; j++) {
				if(i != xLocation || j != yLocation) {
					GridPoint currentPt = new GridPoint(i, j);
					double distance = getGrid().getDistance(location, currentPt);
					total += 1/distance;
				}
			}
		}
		
		return total;
	}
	
	private double calculateGasParticleFactor() {
		List<GasParticle> surroundingGas = findGasAgentsInRadiusOfKnowledge();
		return calculateSurroundingGasParticleFactor(surroundingGas)/calculateWorstCaseScenario();
	}
	
	private double calculateSurroundingGasParticleFactor(List<GasParticle> gasAgents) {
		GridPoint location = getGrid().getLocation(this);
		double totalGasFactor = 0.0;
		
		for(GasParticle gas: gasAgents) {
			GridPoint pt = getGrid().getLocation(gas);
			double distance = getGrid().getDistance(location, pt);
			totalGasFactor += 1/distance;
		}
		
		return totalGasFactor;
	}
	
	public double getPanic() {
		return this.panic;
	}
	
	public void setPanic(double panic) {
		this.panic = panic;
	}
	
	enum PatientMode {
		AVOID_GAS,
		FOLLOW_DOCTOR,
		APPROACH_DOOR
	}
}
