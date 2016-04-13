package bitspls.evacuation;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Door {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Door(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}
