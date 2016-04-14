package bitspls.evacuation.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import bitspls.evacuation.DoctorDoorPoint;
import bitspls.evacuation.Door;
import bitspls.evacuation.DoorPointEnum;
import javafx.util.Pair;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.relogo.Utility;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;
import repast.simphony.query.space.grid.GridCell;

public class Doctor extends Human {

    private DoctorMode doctorMode;
    private static final int SPEED = 1;
    private List<DoctorDoorPoint> doorPoints;
    private int followers;
    private double charisma;
    
    public Doctor(ContinuousSpace<Object> space, Grid<Object> grid, double meanCharisma, double stdCharisma, Random random) {
        this.setSpace(space);
        this.setGrid(grid);
        this.setDead(false);
        this.setRadiusOfKnowledge(15);
        this.setSpeed(SPEED);
        this.doorPoints = new ArrayList<>();
        this.followers = 0;
        this.charisma = stdCharisma * random.nextGaussian() + meanCharisma;
        this.doctorMode = DoctorMode.DOOR_SEEK;
    }
    
    public void addDoor(NdPoint doorPoint, DoorPointEnum status) {
        double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        DoctorDoorPoint ddp = new DoctorDoorPoint(doorPoint, status, ticks);
        this.doorPoints.add(ddp);
    }
    
    @ScheduledMethod(start = 1, interval = SPEED)
    public void run() {
        if (!isDead()) {
            updateDoorKnowledge();
            exchangeInformationWithDoctors();
            if (doctorMode == DoctorMode.PATIENT_SEEK) {
                findPatients();
            } else {
                moveTowardsDoor();
            }
        }
    }

    private void findPatients() {
        moveRandomly();
    }

    private void moveRandomly() {
        GridPoint pt = this.getGrid().getLocation(this);

        List<Integer> options = new ArrayList<Integer>();
        options.add(0);
        options.add(1);
        options.add(-1);
        Collections.shuffle(options);
        
        int xRand = RandomHelper.getUniform().nextIntFromTo(0, 2);
        int xShift = options.get(xRand) * 10;
        Collections.shuffle(options);
        
        int yRand = RandomHelper.getUniform().nextIntFromTo(0, 2);
        int yShift = options.get(yRand) * 10;
        
        GridPoint point = new GridPoint(pt.getX() + xShift, pt.getY() + yShift);
    
        
        super.moveTowards(point);
    }
    
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

    private void moveTowardsDoor() {
        Pair<Double, GridPoint> distancePointPair = findClosestAvailableDoor();
        
        double closestDoorDistance = distancePointPair.getKey();
        GridPoint closestDoorPoint = distancePointPair.getValue();
        
        if (closestDoorDistance < 3) {
            doctorMode = DoctorMode.PATIENT_SEEK;

        }
        
        if (closestDoorPoint != null) {
            moveTowards(closestDoorPoint);
        } else {
            this.kill();
        }
    }

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
        
        System.out.println("num of doors: " + this.doorPoints.size());
    }

    
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
    
    private Boolean isDoorOvercrowded(Door door) {
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
    
    private Boolean isDoorBlocked(Door door) {
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

    public void startFollowing() {
        this.followers++;
        doctorMode = DoctorMode.DOOR_SEEK;
    }
    
    public void stopFollowing() {
        this.followers--;
    }
    
    public int getFollowers() {
        return this.followers;
    }

    public double getCharisma() {
        return this.charisma;
    }
    
    public void setCharisma(double charisma) {
        this.charisma = charisma;
    }
    
    public enum DoctorMode {
        DOOR_SEEK,
        PATIENT_SEEK,
        ESCAPE
    }
}
