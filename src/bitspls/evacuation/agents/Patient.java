package bitspls.evacuation.agents;

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
			
			if (this.doctorToFollow == null || this.door != null) {
				GridCellNgh<Doctor> doctorNghCreator = new GridCellNgh<Doctor>(this.getGrid(), pt, Doctor.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
				List<GridCell<Doctor>> doctorGridCells = doctorNghCreator.getNeighborhood(true);
				SimUtilities.shuffle(doctorGridCells, RandomHelper.getUniform());
				
				
				for (GridCell<Doctor> cell : doctorGridCells) {
					if (cell.size() > 0) {
						for (Doctor doc : cell.items()) {
							this.doctorToFollow = doc;
							this.doctorToFollow.startFollowing();
							this.movementMode = PatientMode.FOLLOW_DOCTOR;
							break;
						}
					}
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
	
	enum PatientMode {
		AVOID_GAS,
		FOLLOW_DOCTOR,
		APPROACH_DOOR
	}
}
 