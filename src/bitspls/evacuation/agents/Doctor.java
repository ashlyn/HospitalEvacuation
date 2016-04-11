package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.relogo.Utility;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Doctor extends Human {
	private static final int SPEED = 1;
	private List<NdPoint> doorPoints;
	
	public Doctor(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.setSpace(space);
		this.setGrid(grid);
		this.setDead(false);
		this.setRadiusOfKnowledge(15);
		this.setSpeed(SPEED);
		this.doorPoints = new ArrayList<>();
	}
	
	public void addDoor(NdPoint doorPoint) {
		this.doorPoints.add(doorPoint);
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
				pt = pointWithLeastGas;
			}
			
			double closestDoorDistance = Double.POSITIVE_INFINITY;
			NdPoint closestDoor = null;
			for (NdPoint doorPoint : doorPoints) {
				double distance = Math.sqrt(Math.pow(doorPoint.getX() - pt.getX(), 2)
						+ Math.pow(doorPoint.getY() - pt.getY(), 2));
				if (distance < closestDoorDistance) {
					closestDoor = doorPoint;
					closestDoorDistance = distance;
				}
			}
			GridPoint closestDoorPoint = Utility.ndPointToGridPoint(closestDoor);
			
			if (closestDoorPoint != null) {
				moveTowards(closestDoorPoint);
			} else {
				this.kill();
			}
		}
	}
}
