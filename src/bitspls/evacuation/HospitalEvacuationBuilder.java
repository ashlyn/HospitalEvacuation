package bitspls.evacuation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.BouncyBorders;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.GasParticle;
import bitspls.evacuation.agents.Patient;

/**
 * @author Bits Please
 * HospitalEvacuationBuilder builds the initial space and grid
 * Doctors, Patients, Gas Particles, and Doors are placed into the space and grid
 * The relevant user parameters are read in from the Repast GUI and used
 * to calculate values (agent counts, starting panic, charisma, etc.)
 */
public class HospitalEvacuationBuilder implements ContextBuilder<Object> {
	/**
	 * Builds the initial are for the hospital evacuation simulation
	 * @param context Context to add space, grid, and agents to
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("HospitalEvacuation");

		Parameters params = RunEnvironment.getInstance().getParameters();
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.BouncyBorders(), new double[] {200, 150},
				new double[] {0, 0});

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new BouncyBorders(),
						new SimpleGridAdder<Object>(), true, new int[] {200, 150}, new int[] {0, 0}));

		int gasCount = 1;
		for (int i = 0; i < gasCount; i++) {
			context.add(new GasParticle(space, grid));
		}
		
		/*
		 * Doors are statically placed along the edges of the space
		 */
		List<Door> doors = new ArrayList<Door>();
		double[][] doorLocations = new double[][]
				{ new double[] { 0.1, 74 },
				new double[] { 199.9, 74 },
				new double[] { 66, 0.1 },
				new double[] { 132, 0.1 },
				new double[] { 66, 149.9 },
				new double[] { 132, 149.9 }};
		
		int overcrowdingThreshold = params.getInteger("overcrowding_threshold");
		int blockedThreshold = params.getInteger("blocked_threshold");
        int doorRadius = params.getInteger("door_radius");
        
        for (double[] location : doorLocations) {
            Door door = new Door(space, grid, doorRadius, overcrowdingThreshold, blockedThreshold);
            context.add(door);
            space.moveTo(door, location);
            doors.add(door);
        }
		
		Random r = new Random();
		
		/*
		 * Generate the number of doctors given by the doctor_count parameter
		 * Doctors are initialized to have a charisma level picked from a
		 * Guassian distribution with a mean of mean_charisma and standard
		 * deviation of std_charisma
		 */
		List<Doctor> doctors = new ArrayList<Doctor>();
		double meanCharisma = params.getDouble("mean_charisma");
		double stdCharisma = params.getDouble("std_charisma");
		int doctorCount = params.getInteger("doctor_count");
		for (int i = 0; i < doctorCount; i++) {
			Doctor doctor = new Doctor(space, grid, meanCharisma, stdCharisma, r);
			context.add(doctor);
			doctors.add(doctor);
		}
		
		/*
		 * Generate the number of patients given by the patient_count parameter
		 * Patients are initialized to have a panic level picked from a
		 * Guassian distribution with a mean of mean_panic and standard
		 * deviation of std_panic
		 */
		double meanPanic = params.getDouble("mean_panic");
		double stdPanic = params.getDouble("std_panic");
		double patientPanicWeight = params.getDouble("patient_weight");
		double gasPanicWeight = params.getDouble("gas_weight");
		int patientCount = params.getInteger("patient_count");
		for (int i = 0; i < patientCount; i++) {
			Patient p = new Patient(space, grid, patientPanicWeight, gasPanicWeight, meanPanic, stdPanic, r);
			context.add(p);
		}

		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		/*
		 * Give doctors knowledge of the closest 3 doors
		 */
		for (Doctor doctor : doctors) {
			findClosestThreeDoors(doctor, doors, space);
		}
				
		return context;
	}
	
	/**
	 * Find the closest 3 doors to a doctor and adds them to it's knowledge
	 * This is one of the few pieces of global knowledge that the doctors have
	 * @param doctor Doctor to give knowledge of doors to
	 * @param doors List of all doors in the context
	 * @param space The continuous space in which the doors are located
	 */
	private void findClosestThreeDoors(Doctor doctor, List<Door> doors, ContinuousSpace<Object> space) {
		double closestDoorDistance = Double.POSITIVE_INFINITY;
		double secondClosestDoorDistance = Double.POSITIVE_INFINITY;
		double thirdClosestDoorDistance = Double.POSITIVE_INFINITY;
		
		NdPoint closestDoor = null,
				secondClosestDoor = null,
				thirdClosestDoor = null;
		
		NdPoint doctorLocation = space.getLocation(doctor);
		
		/*
		 * Iterate through all doors, keeping track of the 3 minimum distances and
		 * the doors at those distances
		 */
		for (Door door : doors) {
			NdPoint point = space.getLocation(door);
			double distance = Math.sqrt(Math.pow(point.getX() - doctorLocation.getX(), 2)
					+ Math.pow(point.getY() - doctorLocation.getY(), 2));
			
			if (distance < closestDoorDistance) {
				thirdClosestDoor = secondClosestDoor;
				thirdClosestDoorDistance = secondClosestDoorDistance;
				secondClosestDoor = closestDoor;
				secondClosestDoorDistance = closestDoorDistance;
				closestDoorDistance = distance;
				closestDoor = point;
			} else if (distance < secondClosestDoorDistance) {
				thirdClosestDoor = secondClosestDoor;
				thirdClosestDoorDistance = secondClosestDoorDistance;
				secondClosestDoorDistance = distance;
				secondClosestDoor = point;
			} else if (distance < thirdClosestDoorDistance) {
				thirdClosestDoorDistance = distance;
				thirdClosestDoor = point;
			}
		}
		
		doctor.addDoor(closestDoor, DoorPointEnum.AVAILABLE);
		doctor.addDoor(secondClosestDoor, DoorPointEnum.AVAILABLE);
		doctor.addDoor(thirdClosestDoor, DoorPointEnum.AVAILABLE);
	}
}
