package bitspls.evacuation;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class GasParticle {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public GasParticle(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}
