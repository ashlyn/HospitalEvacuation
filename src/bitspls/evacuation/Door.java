package bitspls.evacuation;

import java.util.ArrayList;
import java.util.List;

import bitspls.evacuation.agents.Patient;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

/**
 * Class to represent a door in the hospital environment
 * @author Bits Please
 */
public class Door {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private int radius;
    private int overcrowding;
    private int blocked;
    
    /**
	 * Constructor for Door
	 * @param space Space in which the door is located
	 * @param grid Grid in which the door is located
	 * @param radius Radius of knowledge of the door
	 * @param overcrowding Number of patients needed for a door to
	 * be overcrowded
	 * @param blocked Number of gas particles needed to determine
	 * if a door is blocked
	 */
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

    /**
     * Removes patients from the context every tick if there are patients trying to exit
     */
    @ScheduledMethod(start = 1, interval = 1)
    public void allowPatientsToExit() {
        GridPoint pt = this.grid.getLocation(this);
        GridCellNgh<Patient> nghCreator = new GridCellNgh<Patient>(this.grid, pt, Patient.class, 1, 1);
        List<GridCell<Patient>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        List<Patient> patients = new ArrayList<Patient>();
        for (GridCell<Patient> cell : gridCells) {
            for (Patient p : cell.items()) {
                patients.add(p);
            }
        }
        
        Context<Object> context = ContextUtils.getContext(this);
        if (patients.size() > 2) {
            SimUtilities.shuffle(patients, RandomHelper.getUniform());
            Patient p = patients.remove(0);
            context.remove(p);
            p = patients.remove(0);
            context.remove(p);
        } else if (patients.size() > 0) {
            for (Patient p : patients) {
                context.remove(p);
            }
        }
    }
}
