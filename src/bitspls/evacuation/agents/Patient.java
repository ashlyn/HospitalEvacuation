package bitspls.evacuation.agents;

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
	public Patient(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void run() {
		if (!isDead()) {
			GridPoint pt = this.getGrid().getLocation(this);
			
			GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, 10, 10);
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
	
	
}
