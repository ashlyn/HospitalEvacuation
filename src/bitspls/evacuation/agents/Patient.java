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
	
	private Doctor doctorToFollow;
	
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(10);
		this.setSpeed(SPEED);
		this.doctorToFollow = null;
	}
	
	@ScheduledMethod(start = 1, interval = SPEED)
	public void run() {
		if (!isDead()) {
			GridPoint pt = this.getGrid().getLocation(this);
			
			GridCellNgh<Doctor> doctorNghCreator = new GridCellNgh<Doctor>(this.getGrid(), pt, Doctor.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
			List<GridCell<Doctor>> doctorGridCells = doctorNghCreator.getNeighborhood(true);
			SimUtilities.shuffle(doctorGridCells, RandomHelper.getUniform());
			
			GridPoint pointToMoveTo = null;
			GridPoint leastGasPoint = findLeastGasPoint(pt);
			for (GridCell<Doctor> cell : doctorGridCells) {
				if (cell.size() > 0) {
					for (Doctor doc : cell.items()) {
						this.doctorToFollow = doc;
						break;
					}
				}
			}
			
			if (this.doctorToFollow == null || !this.doctorToFollow.isDead()) {
				pointToMoveTo = leastGasPoint;
			} else if (leastGasPoint != null) {
				pointToMoveTo = this.getGrid().getLocation(this.doctorToFollow);
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
}
