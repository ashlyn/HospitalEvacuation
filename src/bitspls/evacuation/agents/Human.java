package bitspls.evacuation.agents;

import java.util.ArrayList;
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

/**
 * Abstract base class to reperesent all human agents in the system
 * 
 * Tracks variables (dead boolean, grid, space, radius of knowledge, etc.)
 * common to all types of human agents and provides methods for moving
 * @author Bits Please
 */
public abstract class Human {
	private static final double MOVEMENT_DISTANCE = 1;
	
	private boolean dead;
	private boolean isGoalSeekState = true;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int radiusOfKnowledge;
	private int speed;
	
	/**
	 * Move towards a given point
	 * If in a "goal-seeking" state (ex. moving towards a door)
	 * navigate around obstacles (areas with gas)
	 * @param pt Point to move towards
	 */
	protected void moveTowards(GridPoint pt) {
		if (pt != null && !pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			
			GridPoint gasToAvoid = gasInWay(angle);
			
			if (isGoalSeekState) {
				if (gasToAvoid != null) {
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
	
	/**
	 * Move along a given angle
	 * @param angle Angle to move the agent along
	 */
	protected void move(double angle) {
		space.moveByVector(this, MOVEMENT_DISTANCE, angle, 0);
		NdPoint point = space.getLocation(this);
		grid.moveTo(this, (int)point.getX(), (int)point.getY());
	}
	
	/**
	 * Find a point that does not contain any gas agents
	 * @param pt Point to center the search around
	 * @return GridPoint that has no gas particles present
	 */
	protected GridPoint findLeastGasPoint(GridPoint pt) {
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
	
	/**
	 * Find any doctors in an agent's neighborhood
	 * @return List of doctors in an agent's neighborhood
	 */
	protected List<Doctor> findDoctorsInRadius() {
		GridPoint currentLocation = this.getGrid().getLocation(this);
		GridCellNgh<Doctor> doctorNghCreator = new GridCellNgh<Doctor>(this.getGrid(), currentLocation, Doctor.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
		List<GridCell<Doctor>> doctorGridCells = doctorNghCreator.getNeighborhood(true);
		SimUtilities.shuffle(doctorGridCells, RandomHelper.getUniform());
		
		List<Doctor> doctorAgents = new ArrayList<Doctor>();
		for (GridCell<Doctor> cell : doctorGridCells) {
			if (cell.size() > 0) {
				for (Doctor doc : cell.items()) {
					doctorAgents.add(doc);
				}
			}
		}
		
		return doctorAgents;
	}
	
	/**
	 * Find the "best" gas point, i.e. the most extreme gas point in the way
	 * of an agent's movement path
	 * Used to keep human agents navigating towards a goal (ex. door), while
	 * avoiding obstacles (ex. gas cloud)
	 * @param angleB Angle at which the agent is attempting to move
	 * @return GridPoint location of the optimal gas particle to avoid
	 */
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
			
				/**
				 * Imagining a circle of r=3 around the agent, split it into 8 45-degree segments
				 */
				if (angleB >= 0 && angleB <= Math.PI / 8) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 8 && angleB <= Math.PI / 4) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, false, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI / 4 && angleB <= Math.PI * 3 / 4) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 4 && angleB <= Math.PI) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX,
							currentBestY, true, true)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI && angleB <= Math.PI * 5 / 4) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY, currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 5 / 4 && angleB <= Math.PI * 3 / 2) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentY, currentX, currentBestY,
							currentBestX, true, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else if (angleB > Math.PI * 3 / 2 && angleB <= Math.PI * 7 / 4) {
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				} else {			
					if (checkIfGasInBounds(currentGasPoint, pt, angleB, currentX, currentY, currentBestX, currentBestY, false, false)) {
						bestGasPoint = currentGasPoint;
					}
				}
			}
		}
		
		return bestGasPoint;
	}
	
	/**
	 * Checks to see if there is a gas agent near the path to
	 * the door from the doctor's current location
	 * @param gas The gas point to compare to the bounds
	 * @param human The location of the doctor
	 * @param angleTowardsDoor The angle the patient must travel on to get to the door
	 * @param verticalLocation The relative vertical location to the doctor
	 * @param horizontalLocation The relative horizontal location to the doctor
	 * @param bestVerticalLocation The current closest gas's vertical location
	 * @param bestHorizontalLocation The current closest gas's horizontal location
	 * @param maximizeVerticalLocation Indicates if we want the max or min vertial
	 * location of the gas particles as our best particle
	 * @param maximizeHorizontalLocation highLength Indicates if we want the max or
	 * min horizontal location of the gas particles as our best particle
	 * @return Whether or not the gas was in the way of the doctor's current path
	 */
	private boolean checkIfGasInBounds(GridPoint gas, GridPoint human, double angleTowardsDoor,
			int verticalLocation, int horizontalLocation, int bestVerticalLocation,
			int bestHorizontalLocation, boolean maximizeVerticalLocation,
			boolean maximizeHorizontalLocation) {
		
		boolean isInBounds = false;
		
		NdPoint humanPoint = new NdPoint(human.getX(), human.getY());
		NdPoint gasPoint = new NdPoint(gas.getX(), gas.getY());
		double angle = SpatialMath.calcAngleFor2DMovement(space, humanPoint, gasPoint);
		
		if (angle >= angleTowardsDoor - (Math.PI / 2) && angle <= angleTowardsDoor + (Math.PI / 2)) {
			 
			if (maximizeVerticalLocation && (verticalLocation > bestVerticalLocation)
					|| (!maximizeVerticalLocation && verticalLocation < bestVerticalLocation)) {
				isInBounds = true;
			} else if (verticalLocation == bestVerticalLocation
					&& (maximizeHorizontalLocation && horizontalLocation > bestHorizontalLocation
							|| (!maximizeHorizontalLocation && horizontalLocation < bestHorizontalLocation))) {
				isInBounds = true;
			} else if (bestVerticalLocation == -1) {
				isInBounds = true;
			}
		}
		
		return isInBounds;
	}
	
	/*
	 * Getters & Setters
	 */
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
