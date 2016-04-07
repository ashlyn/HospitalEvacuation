package bitspls.evacuation;

import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class GasParticle {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public GasParticle(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void spawn() {
		// get grid location of this Gas particle
		GridPoint pt = grid.getLocation(this);
		
		if (pt != null) {
		
			// use GridCellNgh class to create GridCells  for neighborhood
			GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(grid, pt, GasParticle.class, 1, 1);
			
			List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			GridPoint pointWithLeastGas = null;
			int maxCount = Integer.MAX_VALUE;
			for (GridCell<GasParticle> cell : gridCells) {
				if (cell.size() < maxCount) {
					pointWithLeastGas = cell.getPoint();
					maxCount = cell.size();
				}
			}
			
			NdPoint spacePt = new NdPoint(pointWithLeastGas.getX(), pointWithLeastGas.getY());
			Context<Object> context = ContextUtils.getContext(this);
			GasParticle gas = new GasParticle(space, grid);
			context.add(gas);
			space.moveTo(gas, spacePt.getX(), spacePt.getY());
			grid.moveTo(gas, pointWithLeastGas.getX(), pointWithLeastGas.getY());
		}
	}
}
