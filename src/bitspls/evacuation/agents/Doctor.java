package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import bitspls.evacuation.DoctorDoorPoint;
import bitspls.evacuation.Door;
import bitspls.evacuation.DoorPointEnum;
import javafx.util.Pair;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.relogo.Utility;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.query.space.grid.GridCell;

/**
 * Class to model the Doctor agent
 * Doctors initially attempt to avoid gas and when
 * they encounter patients, they lead the patients
 * to one of the doors they have knowledge of, then
 * continue exploring the space
 * 
 * Doctors have the ability to learn about other doors
 * in the system from encountering them or by communicating
 * with other doctors who share their knowledge
 * 
 * Doctors extend Human, which has some mechanics for all
 * human agents in regards to moving & avoiding gas
 * @author Bits Please
 *
 */
public class Doctor extends Human {
	private DoctorMode doctorMode;
	private List<DoctorDoorPoint> doorPoints;
	private int followers;
	private double charisma;
    private GridPoint lastPointMovedTowards;
    
    /**
	 * Constructor for Doctor agent
	 * @param space The continuous space in which the agent is located
	 * @param grid The grid in which the agent is located
	 * @param meanCharisma The mean charisma level for all doctors
	 * @param stdCharisma The standard deviation of charisma for all doctors
	 * @param random An RNG to set this instance's charisma
	 */
    public Doctor(ContinuousSpace<Object> space, Grid<Object> grid, double meanCharisma, double stdCharisma, Random random) {
        this.setSpace(space);
        this.setGrid(grid);
        this.setDead(false);
        this.setRadiusOfKnowledge(15);
        this.doorPoints = new ArrayList<>();
        this.followers = 0;
        this.charisma = getStartingCharisma(meanCharisma, stdCharisma, random);
        this.doctorMode = DoctorMode.DOOR_SEEK;
    }
    
    private double getStartingCharisma(double meanCharisma, double stdCharisma, Random random) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		String charismaDistribution = params.getString("dist_charisma");
		if (charismaDistribution.toUpperCase().equals("CONSTANT")) {
			return meanCharisma;
		}
		else if (charismaDistribution.toUpperCase().equals("UNIFORM")) {
			return random.nextDouble();
		}
		else {
			return stdCharisma * random.nextGaussian() + meanCharisma;
		}
	}
    
    /**
	 * Adds a door to the doctor's knowledge
	 * @param doorPoint Point where the door is located
	 */
    public void addDoor(NdPoint doorPoint, DoorPointEnum status) {
        double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        DoctorDoorPoint ddp = new DoctorDoorPoint(doorPoint, status, ticks);
        this.doorPoints.add(ddp);
    }
    
    /**
	 * Scheduled method to move a doctor if they are still alive
	 */
    @ScheduledMethod(start = 1, interval = 1)
    public void run() {
        if (!isDead()) {
            if(shouldExit()) {
                this.doctorMode = DoctorMode.ESCAPE;
                moveTowardsDoor();
            }
            else {
                updateDoorKnowledge();
                exchangeInformationWithDoctors();
                if (doctorMode == DoctorMode.PATIENT_SEEK) {
                    findPatients();
                } else {
                    moveTowardsDoor();
                }
            }
        }
    }
    
    private boolean shouldExit() {
        if (findNumberOfUnblockedDoors() == 1 || (isGasInRadius(10) && isDoorInRadius(10))) {
            return true;
        }
        
        return false;
    }
    
    private Boolean isDoorInRadius(int radius) {
        Boolean doorIsPresent = false;
        GridPoint location = this.getGrid().getLocation(this);
        GridCellNgh<Door> nghCreator = new GridCellNgh<Door>(this.getGrid(), location, Door.class, radius, radius);
        List<GridCell<Door>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        for (GridCell<Door> cell : gridCells) {
            if (cell.size() > 0) {
                return true;
            }
        }

        return doorIsPresent;
    }
    
    private int findNumberOfUnblockedDoors() {
        int num = 0;
        for(DoctorDoorPoint door: doorPoints) {
            if(door.getStatus() != DoorPointEnum.BLOCKED) {
                num++;
            }
        }
        return num;
    }

    /**
	 * Moves a doctor away from a door if they have just
	 * dropped off patients and moves them randomly to 
	 * explore for more patients if not actively leading.
	 * Attempts to allow doctors to move in a more consistent
	 * manner by following a previous path rather than
	 * choosing a new path.
	 */
	private void findPatients() {
		if (lastPointMovedTowards != null && Math.random() > .15) {
    		super.moveTowards(lastPointMovedTowards);
    	} else {
    		moveRandomly();
    	}
	}

    /**
	 * Move towards a random point
	 */
    private void moveRandomly() {
        GridPoint pt = this.getGrid().getLocation(this);

        List<Integer> options = new ArrayList<Integer>();
        options.add(0);
        options.add(1);
        options.add(-1);
        Collections.shuffle(options);
        
        int xRand = RandomHelper.getUniform().nextIntFromTo(0, 2);
        int xShift = options.get(xRand) * 15;
        Collections.shuffle(options);
        
        int yRand = RandomHelper.getUniform().nextIntFromTo(0, 2);
        int yShift = options.get(yRand) * 15;
        
        GridPoint point = new GridPoint(pt.getX() + xShift, pt.getY() + yShift);
    
        super.moveTowards(point);
        
        if (xShift == 0 && yShift == 0) {
        	lastPointMovedTowards = null;
        } else {
        	lastPointMovedTowards = point;        	
        }
    }
    
    /**
	 * Exchanges information of doors with other doctors
	 * in this doctor's neighborhood
	 */
    private void exchangeInformationWithDoctors() {
        List<Doctor> doctorsInRadius = super.findDoctorsInRadius();
        for(Doctor doc : doctorsInRadius) {
            for(DoctorDoorPoint door : doc.doorPoints) {
                Boolean haveKnowledgeOfDoor = false;
                for(DoctorDoorPoint myDoor: this.doorPoints) {
                    if (myDoor.getPoint() == door.getPoint()) {
                        if (myDoor.getLastVisitedTime() < door.getLastVisitedTime()) {
                            myDoor.setStatus(door.getStatus());
                            myDoor.setLastVisitedtime(door.getLastVisitedTime());
                        }
                        haveKnowledgeOfDoor = true;
                    }
                }
                
                if (!haveKnowledgeOfDoor) {
                    this.doorPoints.add(door);
                }
            }
        }
    }

    /**
	 * Move towards a the closest available (unblocked and not overcrowded)
	 * door if leading patients
	 */
    private void moveTowardsDoor() {
        Pair<Double, GridPoint> distancePointPair = findClosestAvailableDoor();
        
        double closestDoorDistance = distancePointPair.getKey();
        GridPoint closestDoorPoint = distancePointPair.getValue();
        
        if (closestDoorDistance < 3) {
            if(isGasInRadius(5)) {
                doctorMode = DoctorMode.ESCAPE;
            }
            else {
                doctorMode = DoctorMode.PATIENT_SEEK;
            }
        }
        
        if (closestDoorPoint != null) {
            moveTowards(closestDoorPoint);
        } else {
            this.kill();
        }
    }
    
    /**
     * Check if gas is near the door
     * @param radius The radius to check for gas
     * @return Where gas is near the door
     */
    private boolean isGasInRadius(int radius) {
        Boolean gasIsPresent = false;
        GridPoint location = this.getGrid().getLocation(this);
        GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), location, GasParticle.class, radius, radius);
        List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        for (GridCell<GasParticle> cell : gridCells) {
            if (cell.size() > 0) {
                return true;
            }
        }

        return gasIsPresent;
    }

    /**
     * Updates the doctors knowledge of specific doors by updating 
     * the status of doors or adding the doors to the list of
     * known doors for the doctor
     */
    private void updateDoorKnowledge() {
        List<Door> doorsInRadius = findDoorsInRadius();

        for(Door door: doorsInRadius) 
        {
            GridPoint gridLocation = this.getGrid().getLocation(door);
            NdPoint location = new NdPoint(gridLocation.getX(), gridLocation.getY());
            DoctorDoorPoint targetDoor = null;
            
            //Check if door is in list of known doors
            for(DoctorDoorPoint doorPoint: this.doorPoints) 
            {
                if (doorPoint.getPoint().getX() == location.getX()
                    && doorPoint.getPoint().getY() == location.getY()) {
                    targetDoor = doorPoint;
                }
            }
            double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
            
            //door is not in list of known doors
            if (targetDoor == null) {
                targetDoor = new DoctorDoorPoint(location, DoorPointEnum.AVAILABLE, ticks);
                this.doorPoints.add(targetDoor);
            }
            
            if (this.isDoorBlocked(door)) 
            {
                targetDoor.setStatus(DoorPointEnum.BLOCKED);
            }
            else if (this.isDoorOvercrowded(door)) 
            {
                targetDoor.setStatus(DoorPointEnum.OVERCROWDED);
            }
            targetDoor.setLastVisitedtime(ticks);
        }
    }

    /**
     * Determine if there are new doors nearby that can be used
     * in the future
     * @return The list of doors near the doctor
     */
    private List<Door> findDoorsInRadius() {
        GridPoint location = this.getGrid().getLocation(this);
        GridCellNgh<Door> nghCreator = new GridCellNgh<Door>(this.getGrid(), location, Door.class, this.getRadiusOfKnowledge(), this.getRadiusOfKnowledge());
        List<GridCell<Door>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        List<Door> doors = new ArrayList<Door>();
        
        for (GridCell<Door> cell : gridCells) {
            if (cell.size() > 0) {
                for(Door door : cell.items()) {
                    doors.add(door);
                }
            }
        }
        
        return doors;
    }
    
    /**
     * Checks if the number of patients near a door exceeds the threshold
     * for that door
     * @param door The door to check for overcrowding
     * @return boolean indication whether the door is overcrowded
     */
    private boolean isDoorOvercrowded(Door door) {
        GridPoint location = this.getGrid().getLocation(door);
        GridCellNgh<Patient> nghCreator = new GridCellNgh<Patient>(this.getGrid(), location, Patient.class, door.getRadius(), door.getRadius());
        List<GridCell<Patient>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        int numOfPatients = 0;
        for (GridCell<Patient> cell : gridCells) {
            if (cell.size() > 0) {
                numOfPatients++;
            }
        }
        
        if (numOfPatients < door.getOvercrowdingThreshold()) {
            return false;
        }
        return true;
    }
    
    /**
     * Checks if a door is blocked by gas by comparing the number of gas
     * particles near the door to the blocking threshold
     * @param door The door to check
     * @return Whether the door is blocked
     */
    private boolean isDoorBlocked(Door door) {
        GridPoint location = this.getGrid().getLocation(door);
        GridCellNgh<GasParticle> nghCreator = new GridCellNgh<GasParticle>(this.getGrid(), location, GasParticle.class, door.getRadius(), door.getRadius());
        List<GridCell<GasParticle>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
        
        int numOfGasParticles = 0;
        for (GridCell<GasParticle> cell : gridCells) {
            if (cell.size() > 0) {
                numOfGasParticles++;
            }
        }
        
        if (numOfGasParticles < door.getBlockedThreshold()) {
            return false;
        }
        
        return true;
    }
    
    /**
	 * Finds the closest available door to this doctor
	 * @return A key-value pair of the distance and the grid point corresponding to the door
	 */
    private Pair<Double, GridPoint> findClosestAvailableDoor() {
        GridPoint pt = this.getGrid().getLocation(this);
        
        double closestDoorDistance = Double.POSITIVE_INFINITY;
        NdPoint closestDoor = null;
        for (DoctorDoorPoint doorPoint : doorPoints) {
            double distance = Math.sqrt(Math.pow(doorPoint.getPoint().getX() - pt.getX(), 2)
                    + Math.pow(doorPoint.getPoint().getY() - pt.getY(), 2));
            if (distance < closestDoorDistance && doorPoint.getStatus() == DoorPointEnum.AVAILABLE) {
                closestDoor = doorPoint.getPoint();
                closestDoorDistance = distance;
            }
        }
        
        //No available doors, check overcrowded ones
        if (closestDoor == null) {
            closestDoor = findClosestOvercrowdedDoor();
        }
        
        //All doors are blocked
        if (closestDoor == null) {
            
        }
        
        GridPoint closestDoorPoint = Utility.ndPointToGridPoint(closestDoor);
        
        return new Pair<Double, GridPoint>(closestDoorDistance, closestDoorPoint);
    }
    
    /**
     * Look for a door that was previously overcrowded and attempt to use it
     * @return The location of the door to use
     */
    private NdPoint findClosestOvercrowdedDoor() {
        NdPoint closestDoor = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        GridPoint pt = this.getGrid().getLocation(this);
        
        for(DoctorDoorPoint door : doorPoints) {
            GridPoint doorPoint = Utility.ndPointToGridPoint(door.getPoint());
            double distance = this.getGrid().getDistance(doorPoint, pt);
            if (distance < closestDistance) {
                closestDoor = door.getPoint();
            }
        }
        
        return closestDoor;
    }

    /**
	 * Begin following this doctor
	 * Increment the followers counter and send the doctor towards a door
	 */
	public void startFollowing() {
		this.followers++;
		doctorMode = DoctorMode.DOOR_SEEK;
	}
	
	/**
	 * Stop following a doctor. If that was the last patient following,
	 * send the doctor to find more patients
	 */
	public void stopFollowing() {
		this.followers--;
		if (followers <= 0) {
			doctorMode = DoctorMode.PATIENT_SEEK;
		}
	}
	
	/*
	 * Getters and setters for charisma, followers
	 */
	public int getFollowers() {
		return this.followers;
	}

    public double getCharisma() {
        return this.charisma;
    }
    
    public void setCharisma(double charisma) {
        this.charisma = charisma;
    }

    /**
     * Kill a doctor by removing it from the context
     */
    public void kill() {
    	super.kill();
    	Context<Object> context = ContextUtils.getContext(this);
    	int humanCount = context.getObjects(Doctor.class).size() + context.getObjects(Patient.class).size();
    	
    	if (humanCount > 1) {
	    	GridPoint pt = this.getGrid().getLocation(this);
	    	NdPoint spacePt = new NdPoint(pt.getX(), pt.getY());
	
			DeadDoctor deadDoctor = new DeadDoctor();
			context.add(deadDoctor);
			this.getSpace().moveTo(deadDoctor, spacePt.getX(), spacePt.getY());
			this.getGrid().moveTo(deadDoctor, pt.getX(), pt.getY());
			
			context.remove(this);
    	} else {
    		RunEnvironment.getInstance().endRun();
    	}
    }
    
    public DoctorMode getMode() {
        return this.doctorMode;
    }
	
	/**
	 * Enum to represent the state of a doctor
	 * @author Bits Please
	 */
	public enum DoctorMode {
		DOOR_SEEK,  	// should move towards closest available door
		PATIENT_SEEK,	// should look for patients while avoiding gas
		ESCAPE			// move to door and exit
	}
}
