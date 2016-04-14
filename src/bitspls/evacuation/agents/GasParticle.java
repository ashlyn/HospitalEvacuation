package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
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

/**
 * Class to represent simple gas particle agent
 * Gas particles only spread and poison human agents
 * @author Bits Please
 */
public class GasParticle {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	boolean moved = false;
	
	/**
	 * Constructor for gas particle agent
	 * @param space Continuous space the gas is located in
	 * @param grid Grid the gas is located in
	 */
	public GasParticle(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	/**
	 * Scheduled method to allow the gas particle to spawn into an adjacent square
	 * if it has not already done so
	 * Also wraps the method that checks if there are human agents nearby to poison
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void spawn() {
		int ticks = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();

		// Only spawn on certain ticks
		if (!moved && ticks % 10 == 0) {
		GridPoint pt = grid.getLocation(this);
			if (pt != null) {
			
				// use GridCellNgh class to create GridCells  for neighborhood
				GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(grid, pt, GasParticle.class, 1, 1);
				
				List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
				SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
				
				// Find a point that does not already have gas in it
				GridPoint pointWithLeastGas = null;
				for (GridCell<GasParticle> cell : gridCells) {
					if (cell.size() == 0) {
						pointWithLeastGas = cell.getPoint();
					}
				}
				
				// Spawn a new gas particle into that point
				if (pointWithLeastGas != null) {
					NdPoint spacePt = new NdPoint(pointWithLeastGas.getX(), pointWithLeastGas.getY());
					Context<Object> context = ContextUtils.getContext(this);
					GasParticle gas = new GasParticle(space, grid);
					context.add(gas);
					space.moveTo(gas, spacePt.getX(), spacePt.getY());
					grid.moveTo(gas, pointWithLeastGas.getX(), pointWithLeastGas.getY());
				}
			}
		}
		
		// Posion humans if available
		poison();
	}
	
	/**
	 * Posions any humans occupying the same grid point as the gas agent
	 */
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
