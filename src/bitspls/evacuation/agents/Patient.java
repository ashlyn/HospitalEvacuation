package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import bitspls.evacuation.Door;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.GasParticle;
import bitspls.evacuation.agents.Human;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Patient extends Human {
	private static final int SPEED = 2;
	private double panic = 0.5;
	
	private PatientMode movementMode;
	private Doctor doctorToFollow;
	private Door door;
	private boolean exited;
	
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.setSpeed(SPEED);
		this.movementMode = PatientMode.AVOID_GAS;
		this.doctorToFollow = null;
		this.door = null;
		this.exited = false;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		if (!isDead() && !this.exited) {
			GridPoint pt = this.getGrid().getLocation(this);
			GridPoint pointToMoveTo = null;
			GridPoint leastGasPoint = findLeastGasPoint(pt);
			
			if (this.doctorToFollow == null) {
				GridCellNgh<Doctor> doctorNghCreator = new GridCellNgh<Doctor>(this.getGrid(), pt, Doctor.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
				List<GridCell<Doctor>> doctorGridCells = doctorNghCreator.getNeighborhood(true);
				SimUtilities.shuffle(doctorGridCells, RandomHelper.getUniform());
				
				double maxCharisma = 0.0;
				Doctor targetDoctor = null;
				for (GridCell<Doctor> cell : doctorGridCells) {
					if (cell.size() > 0) {
						for (Doctor doc : cell.items()) {
							System.out.println("Charisma:  " + doc.getCharisma());
							if(doc.getCharisma() > maxCharisma) {
								targetDoctor = doc;
								maxCharisma = doc.getCharisma();
							}
						}
					}
				}
				
				if(targetDoctor != null && shouldFollowDoctorAgent(targetDoctor)) {
					System.out.println("Followed Doctor");
					this.doctorToFollow = targetDoctor;
					this.doctorToFollow.startFollowing();
					this.movementMode = PatientMode.FOLLOW_DOCTOR;
				}
			}
			
			if (this.movementMode != PatientMode.APPROACH_DOOR) {
				GridCellNgh<Door> doorNghCreator = new GridCellNgh<Door>(this.getGrid(), pt, Door.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
				List<GridCell<Door>> doorGridCells = doorNghCreator.getNeighborhood(true);
				
				for (GridCell<Door> cell : doorGridCells) {
					if (cell.size() > 0) {
						for (Door door : cell.items()) {
							this.door = door;
							this.movementMode = PatientMode.APPROACH_DOOR;
							break;
						}
					}
				}
			}
			
			if (this.movementMode == PatientMode.APPROACH_DOOR) {
				pointToMoveTo = this.getGrid().getLocation(this.door);
			} else if (this.movementMode == PatientMode.FOLLOW_DOCTOR && !this.doctorToFollow.isDead()) {
				pointToMoveTo = this.getGrid().getLocation(this.doctorToFollow);
			} else if (leastGasPoint != null) {
				pointToMoveTo = leastGasPoint;
			}
			
			if (pointToMoveTo != null) {
				moveTowards(pointToMoveTo);
			} else {
				this.kill();
			}
		}
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
		System.out.println("Probability:  " + probabilityOfFollowingDoctor);
		return randomFollowGenerator(probabilityOfFollowingDoctor);
	}
	
	public boolean randomFollowGenerator(double probabilityTrue)
	{
	    return Math.random() >= 1.0 - probabilityTrue;
	}
	
	public double calculateNewPanicLevel() {
		return (0.4 * getPanic()) + (0.3 * calculatePatientsPanicFactor()) + (0.3 * calculateGasParticleFactor() );
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
 
