package bitspls.evacuation.agents;

import java.util.ArrayList;
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
	boolean moved = false;
	
	public GasParticle(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 1, interval = 10)
	public void spawn() {
		// get grid location of this Gas particle
		if (!moved) {
		GridPoint pt = grid.getLocation(this);
		
			if (pt != null) {
			
				// use GridCellNgh class to create GridCells  for neighborhood
				GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(grid, pt, GasParticle.class, 1, 1);
				
				List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
				SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
				
				GridPoint pointWithLeastGas = null;
				for (GridCell<GasParticle> cell : gridCells) {
					if (cell.size() == 0) {
						pointWithLeastGas = cell.getPoint();
					}
				}
				
				if (pointWithLeastGas != null) {
					NdPoint spacePt = new NdPoint(pointWithLeastGas.getX(), pointWithLeastGas.getY());
					Context<Object> context = ContextUtils.getContext(this);
					GasParticle gas = new GasParticle(space, grid);
					context.add(gas);
					space.moveTo(gas, spacePt.getX(), spacePt.getY());
					grid.moveTo(gas, pointWithLeastGas.getX(), pointWithLeastGas.getY());
					poison();
				}
			}
		}
	}
	
	public void poison() {
		GridPoint pt = grid.getLocation(this);
		List<Human> humans = new ArrayList<Human>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Human) {
				humans.add((Human) obj);
			}
		}
		
		for (int i = 0; i < humans.size(); i++) {
			humans.get(i).kill();
		}
	}
}
