package bitspls.evacuation.agents;

import java.util.ArrayList;
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
import repast.simphony.space.SpatialMath;
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
    private int stepsTakenAwayFromDoor;
    
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
        this.stepsTakenAwayFromDoor = 0;
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
        if (stepsTakenAwayFromDoor < 30) {
            moveAwayFromDoor();
        } else {
            moveRandomly();
        }
    }
    
    private void exchangeInformationWithDoctors() {
        List<Doctor> doctorsInRadius = super.findDoctorsInRadius();
        for(Doctor doc : doctorsInRadius) {
            for(DoctorDoorPoint door : doc.doorPoints) {
                Boolean haveKnowledgeOfDoor = false;
                for(DoctorDoorPoint myDoor: this.doorPoints) {
                    if (myDoor == door) {
                        if (myDoor.getLastVisitedTime() < door.getLastVisitedTime()) {
                            myDoor.setStatus(door.getStatus());
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
    
    private void updateDoorKnowledge() {
        List<Door> doorsInRadius = findDoorsInRadius();
        System.out.println("num of doors in radius: " + doorsInRadius.size());
        
        for(Door door: doorsInRadius) 
        {
            GridPoint gridLocation = this.getGrid().getLocation(door);
            NdPoint location = new NdPoint(gridLocation.getX(), gridLocation.getY());
            DoctorDoorPoint targetDoor = null;
            
            //Check if door is in list of known doors
            for(DoctorDoorPoint doorPoint: this.doorPoints) 
            {
                if (doorPoint.getPoint() == location) {
                    targetDoor = doorPoint;
                }
            }
            
            //door is not in list of known doors
            if (targetDoor == null) {
                double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
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
            
        }
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
    
    private void moveRandomly() {
        GridPoint pt = this.getGrid().getLocation(this);
        GridPoint pointToMoveTo = super.findLeastGasPoint(pt);
        
        super.moveTowards(pointToMoveTo);
    }
    
    private void moveAwayFromDoor() {
        this.stepsTakenAwayFromDoor++;
        Pair<Double, GridPoint> distancePointPair = findClosestAvailableDoor();
        GridPoint doorLocation = distancePointPair.getValue();
        
        NdPoint myPoint = getSpace().getLocation(this);
        NdPoint otherPoint = new NdPoint(doorLocation.getX(), doorLocation.getY());
        double angleAwayFromDoor = SpatialMath.calcAngleFor2DMovement(getSpace(), myPoint, otherPoint);
        
        angleAwayFromDoor -= angleAwayFromDoor > Math.PI ? Math.PI : -Math.PI; 
        
        move(angleAwayFromDoor);
    }
    
    private void moveTowardsDoor() {
        Pair<Double, GridPoint> distancePointPair = findClosestAvailableDoor();
        
        double closestDoorDistance = distancePointPair.getKey();
        GridPoint closestDoorPoint = distancePointPair.getValue();
        
        if (closestDoorDistance < 3) {
            doctorMode = DoctorMode.PATIENT_SEEK;
            this.stepsTakenAwayFromDoor = 0;
        }
        
        if (closestDoorPoint != null) {
            moveTowards(closestDoorPoint);
        } else {
            this.kill();
        }
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
        GridPoint closestDoorPoint = Utility.ndPointToGridPoint(closestDoor);
        
        return new Pair<Double, GridPoint>(closestDoorDistance, closestDoorPoint);
    }
    
    private GridPoint findNextLocation() {
        
        
        return null;
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
