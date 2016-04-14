package bitspls.evacuation;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Door {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private int radius;
    private int overcrowding;
    
    public Door(ContinuousSpace<Object> space, Grid<Object> grid, int radius, int overcrowding) {
        this.space = space;
        this.grid = grid;
        this.radius = radius;
        this.overcrowding = overcrowding;
    }
    
    public int getRadius() {
        return this.radius;
    }
    
    public int getOvercrowdingThreshold() {
        return this.overcrowding;
    }
}
