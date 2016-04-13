package bitspls.evacuation.agents;

import java.util.List;

import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

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
					angle -= Math.PI * 3 / 4;
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
		
		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, 2, 2);
		
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);

		double angleA = Math.PI / 2;
		
		double angleC = 2 * Math.PI - angleA - angleB;
		
		double sinA = 1/2;
		
		double sideBLength = Math.sin(angleB);
		double sideCLength = Math.sin(angleC);
		
		int x = pt.getX();
		int y = pt.getY();
		
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
					if (checkIfGasInBounds(currentY, y - MOVEMENT_DISTANCE, y + MOVEMENT_DISTANCE, currentX,
							x, x + MOVEMENT_DISTANCE, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 8 && angleB <= Math.PI / 4) {
					// check between 0 and 135
					if (checkIfGasInBounds(currentY, y, y + MOVEMENT_DISTANCE, currentX, x - MOVEMENT_DISTANCE,
							x + MOVEMENT_DISTANCE, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 4 && angleB <= Math.PI * 3 / 4) {
					// check between 45 and 180
					if (checkIfGasInBounds(currentX, x - MOVEMENT_DISTANCE, x + MOVEMENT_DISTANCE,
							currentY, y, y + MOVEMENT_DISTANCE, currentBestX, currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 4 && angleB <= Math.PI) {
					if (checkIfGasInBounds(currentX, x - MOVEMENT_DISTANCE, x, currentY,
							y - MOVEMENT_DISTANCE, y + MOVEMENT_DISTANCE, currentBestX,
							currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI && angleB <= Math.PI * 5 / 4) {
					// check between 135 and 270
					if (checkIfGasInBounds(currentY, y - MOVEMENT_DISTANCE, y, currentX,
							x - MOVEMENT_DISTANCE, x, currentBestY, currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 5 / 4 && angleB <= Math.PI * 3 / 2) {
					// check between 180 and 315
					if (checkIfGasInBounds(currentY, y - MOVEMENT_DISTANCE, y, currentX,
							x - MOVEMENT_DISTANCE, x + MOVEMENT_DISTANCE, currentBestY,
							currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 2 && angleB <= Math.PI * 7 / 4) {
					// check between 225 and 0
					if (checkIfGasInBounds(currentY, y - MOVEMENT_DISTANCE, y, currentX, x - MOVEMENT_DISTANCE,
							x + MOVEMENT_DISTANCE, currentBestY, currentBestX, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else {
					// check between 270 and 45					
					if (checkIfGasInBounds(currentY, y - MOVEMENT_DISTANCE, y, currentX,
							x, x + MOVEMENT_DISTANCE, currentBestY, currentBestX, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				}
			}
		}
		
		return bestGasPoint;
	}
	
	private boolean checkIfGasInBounds(int height, double heightLower, double heightHigher, int length,
			double lengthLower, double lengthHigher, int bestHeight, int bestLength, boolean highHeight,
			boolean highLength) {
		
		boolean isInBounds = false;
		
		if (height >= heightLower && height <= heightHigher && length >= lengthLower
				&& length <= lengthHigher) {
			 
			if (highHeight && (height > bestHeight) || (!highHeight && height < bestHeight)) {
				isInBounds = true;
			} else if (height == bestHeight && (highLength && length > bestLength)
					|| (!highLength && length < bestLength)) {
				isInBounds = true;
			}
		}
		
		return isInBounds;
	}
	
	private double computeAvoidanceAngle(double angle, GridPoint gasToAvoid) {
		GridPoint pt = this.getGrid().getLocation(this);
		
		GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), pt, GasParticle.class, 2, 2);
		
		List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);

		double angleInDegrees = angle * 180 / Math.PI;
		double remainingAngle = 270 - angleInDegrees;
		
		double sinA = Math.sin(90);
		
		double sideLength = 2 / sinA * Math.sin(angleInDegrees);
		double otherSideLength = 2 / sinA * Math.sin(remainingAngle);
		
		// TODO figure out correct signs
		if (angleInDegrees >= 0 && angleInDegrees <= 90) {
			// B is y distance
			// C is x distance
		} else if (angleInDegrees > 90 && angleInDegrees <= 180) {
			// B is x distance
			// C is y distance
		} else if (angleInDegrees > 180 && angleInDegrees <= 270) {
			// B is y distance
			// C is x distance
		} else {
			// B is x distance
			// C is y distance
		}
		
		return -1;
	}
	
	protected boolean isDead() {
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
