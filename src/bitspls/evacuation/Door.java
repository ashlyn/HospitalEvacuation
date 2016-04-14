package bitspls.evacuation;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import bitspls.evacuation.agents.DeadDoctor;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.Doctor.DoctorMode;
import bitspls.evacuation.agents.Patient;

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
     * Removes patients or doctors from the context every tick if there
     * are patients or doctors trying to exit
     */
    @ScheduledMethod(start = 1, interval = 1)
    public void allowPatientsOrDoctorsToExit() {
        List<Patient> patients = findExitingPatients();
        allowPatientsToExit(patients);
        
        List<Doctor> doctors = findExitingDoctors();
        if(patients.size() == 0) {
            allowDoctorsToExit(doctors);
        }
    }
    
    /**
     * Attempt to find patients trying to exit
     * @return The list of patients trying to exit
     */
    private List<Patient> findExitingPatients() {
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
        
        return patients;
    }
    
    /**
     * Attempt to find the exiting doctors
     * @return The list of doctors trying to exit
     */
    private List<Doctor> findExitingDoctors() {
        GridPoint pt = this.grid.getLocation(this);
        GridCellNgh<Doctor> nghCreator = new GridCellNgh<Doctor>(this.grid, pt, Doctor.class, 1, 1);
        List<GridCell<Doctor>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        List<Doctor> doctors = new ArrayList<Doctor>();
        for (GridCell<Doctor> cell : gridCells) {
            for (Doctor d : cell.items()) {
                if(d.getMode() == DoctorMode.ESCAPE) {
                    doctors.add(d);
                }
            }
        }
        
        return doctors;
    }
    
    /** 
     * Randomly select and remove two patients who are trying to exit
     * @param patients The list of patients trying to exit
     */
    private void allowPatientsToExit(List<Patient> patients) {
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
        
    	int humanCount = context.getObjects(Doctor.class).size() + context.getObjects(Patient.class).size();
    	if (humanCount == 0) {
    		RunEnvironment.getInstance().endRun();
    	}
    }
    
    /**
     * Randomly select and remove two doctors who are trying to exit
     * @param doctors The list of doctors trying to exit
     */
    private void allowDoctorsToExit(List<Doctor> doctors) {
        Context<Object> context = ContextUtils.getContext(this);
        if (doctors.size() > 2) {
            SimUtilities.shuffle(doctors, RandomHelper.getUniform());
            Doctor d = doctors.remove(0);
            context.remove(d);
            d = doctors.remove(0);
            context.remove(d);
        } else if (doctors.size() > 0) {
            for (Doctor d : doctors) {
                context.remove(d);
            }
        }
        
        int humanCount = context.getObjects(Doctor.class).size() + context.getObjects(Patient.class).size();
    	if (humanCount == 0) {
    		RunEnvironment.getInstance().endRun();
    	}
    }
}
