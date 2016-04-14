package bitspls.evacuation;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * Class to represent a door in the hospital environment
 * @author Bits Please
 */
public class Door {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	/**
	 * Constructor for Door
	 * @param space Space in which the door is located
	 * @param grid Grid in which the door is located
	 */
	public Door(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}
