package bitspls.evacuation;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Door {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private int radius;
    private int overcrowding;
    private int blocked;
    
    public Door(ContinuousSpace<Object> space, Grid<Object> grid, int radius, int overcrowding, int blocked) {
        this.space = space;
        this.grid = grid;
        this.radius = radius;
        this.overcrowding = overcrowding;
        this.blocked = blocked;
    }
    
    public int getRadius() {
        return this.radius;
    }
    
    public int getOvercrowdingThreshold() {
        return this.overcrowding;
    }
    
    public int getBlockedThreshold() {
        return this.blocked;
    }
}
