package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

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
	private double panic = 0.5;
	
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.setSpeed(SPEED);
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
			
			if (pointWithLeastGas != null) {
				moveTowards(pointWithLeastGas);
			} else {
				this.kill();
			}
		}
	}
	
	public Boolean shouldFollowDoctorAgent(Doctor doctor) {
		double probabilityOfFollowingDoctor = 0.4*doctor.getCharisma() + 0.6*getPanic();
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
		int xLocation = location.getX();
		int yLocation = location.getY();
		
		List<GasParticle> gasAgentsInRadius = new ArrayList<GasParticle>();
		
		for(int i = xLocation - radiusOfKnowledge; i < xLocation + radiusOfKnowledge; i++) {
			for(int j = yLocation - radiusOfKnowledge; j < yLocation + radiusOfKnowledge; j++) {
				if(i != xLocation || j != yLocation) {
					for (Object obj : getGrid().getObjectsAt(i, j)) {
						if (obj instanceof GasParticle) {
							gasAgentsInRadius.add((GasParticle) obj);
						}
					}
				}
			}
		}
		return gasAgentsInRadius;
	}
	
	private List<Patient> findPatientAgentsInRadiusOfKnowledge() {
		int radiusOfKnowledge = getRadiusOfKnowledge();
		GridPoint location = getGrid().getLocation(this);
		int xLocation = location.getX();
		int yLocation = location.getY();
		
		List<Patient> patientsInRadius = new ArrayList<Patient>();
		
		for(int i = xLocation - radiusOfKnowledge; i < xLocation + radiusOfKnowledge; i++) {
			for(int j = yLocation - radiusOfKnowledge; j < yLocation + radiusOfKnowledge; j++) {
				if(i != xLocation || j != yLocation) {
					for (Object obj : getGrid().getObjectsAt(i, j)) {
						if (obj instanceof Patient) {
							patientsInRadius.add((Patient) obj);
						}
					}
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
	
}
