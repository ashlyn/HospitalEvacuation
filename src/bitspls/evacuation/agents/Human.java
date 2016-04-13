package bitspls.evacuation.agents;

import java.util.List;

import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public abstract class Human {
	private static final double MOVEMENT_DISTANCE = 1;
	
	private boolean dead;
	private boolean isGoalSeekState;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int radiusOfKnowledge;
	private int speed;
	
	protected void moveTowards(GridPoint pt) {
		if (pt != null && !pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			
			GridPoint gasToAvoid = gasInWay(angle);
			
			if (isGoalSeekState) {
				if (gasToAvoid != null) {
					System.out.println("Gas in way");
					isGoalSeekState = false;
				}
			}
			
			if (!isGoalSeekState) {
				if (gasToAvoid == null) {
					isGoalSeekState = true;
				} else {
					NdPoint point = new NdPoint(gasToAvoid.getX(), gasToAvoid.getY());
					angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, point);
					if ((angle <= Math.PI && angle >= 0) || (angle >= -2 * Math.PI && angle <= -Math.PI)) {
						angle += (Math.PI * 3 / 4);
					} else {
						angle -= (Math.PI * 3 / 4);
					}
				}
			}
			
			move(angle);
		}
	}
	
	private void move(double angle) {
		space.moveByVector(this, MOVEMENT_DISTANCE, angle, 0);
		NdPoint point = space.getLocation(this);
		grid.moveTo(this, (int)point.getX(), (int)point.getY());
	}
	
	private GridPoint gasInWay(double angleB) {
		GridPoint pt = this.getGrid().getLocation(this);
		
		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, 3, 3);
		
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
		
		int currentBestX = -1;
		int currentBestY = -1;
		GridPoint bestGasPoint = null;
		GridPoint currentGasPoint;
		for (GridCell<GasParticle> cell : gridCells) {
			if (cell.size() > 0) {
				currentGasPoint = cell.getPoint();
				int currentX = currentGasPoint.getX();
				int currentY = currentGasPoint.getY();
			
				if (angleB >= 0 && angleB <= Math.PI / 8) {
					// check between 315 and 90
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 8 && angleB <= Math.PI / 4) {
					// check between 0 and 135
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 4 && angleB <= Math.PI * 3 / 4) {
					// check between 45 and 180
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 4 && angleB <= Math.PI) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX,
							currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI && angleB <= Math.PI * 5 / 4) {
					// check between 135 and 270
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 5 / 4 && angleB <= Math.PI * 3 / 2) {
					// check between 180 and 315
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY,
							currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 2 && angleB <= Math.PI * 7 / 4) {
					// check between 225 and 0
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else {
					// check between 270 and 45					
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				}
			}
		}
		
		return bestGasPoint;
	}
	
	private boolean checkIfGasInBounds(GridPoint gas, GridPoint human, double originalAngle,
			int height, int length, int bestHeight, int bestLength, boolean highHeight,
			boolean highLength) {
		
		boolean isInBounds = false;
		
		NdPoint humanPoint = new NdPoint(human.getX(), human.getY());
		NdPoint gasPoint = new NdPoint(gas.getX(), gas.getY());
		double angle = SpatialMath.calcAngleFor2DMovement(space, humanPoint, gasPoint);
		
		if (angle >= originalAngle - (Math.PI / 2) && angle <= originalAngle + (Math.PI / 2)) {
			 
			if (highHeight && (height > bestHeight) || (!highHeight && height < bestHeight)) {
				isInBounds = true;
			} else if (height == bestHeight && (highLength && length > bestLength)
					|| (!highLength && length < bestLength)) {
				isInBounds = true;
			} else if (bestHeight == -1) {
				isInBounds = true;
			}
		}
		
		return isInBounds;
	}
	
	public boolean isDead() {
		return this.dead;
	}
	
	protected void setDead(boolean dead) {
		this.dead = dead;
	}
	
	protected void kill() {
		this.dead = true;
	}
	
	protected ContinuousSpace<Object> getSpace() {
		return this.space;
	}
	
	protected void setSpace(ContinuousSpace<Object> space) {
		this.space = space;
	}
	
	protected Grid<Object> getGrid() {
		return this.grid;
	}
	
	protected void setGrid(Grid<Object> grid) {
		this.grid = grid;
	}
	
	protected int getRadiusOfKnowledge() {
		return this.radiusOfKnowledge;
	}
	
	protected void setRadiusOfKnowledge(int r) {
		this.radiusOfKnowledge = r;
	}
	
	protected int getSpeed() {
		return this.speed;
	}
	
	protected void setSpeed(int speed) {
		this.speed = speed;
	}
}
