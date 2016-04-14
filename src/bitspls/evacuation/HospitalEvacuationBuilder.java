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

public class HospitalEvacuationBuilder implements ContextBuilder<Object> {

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
		
		List<Door> doors = new ArrayList<Door>();
		double[][] doorLocations = new double[][]
				{ new double[] { 80, 149.9 },
				new double[] { 20, 149.9 },
				new double[] { 199.9, 28 },
				new double[] { 0.1, 27 },
				new double[] { 57, 0.1 }};
		
		int overcrowdingThreshold = params.getInteger("overcrowding_threshold");
		int blockedThreshold = params.getInteger("blocked_threshold");
        int doorRadius = params.getInteger("door_radius");
        
        int doorCount = params.getInteger("door_count");
        for (int i = 0; i < doorCount; i++) {
            Door door = new Door(space, grid, doorRadius, overcrowdingThreshold, blockedThreshold);
            context.add(door);
            space.moveTo(door, doorLocations[i]);
            doors.add(door);
        }
		
		Random r = new Random();
		
		List<Doctor> doctors = new ArrayList<Doctor>();
		double meanCharisma = params.getDouble("mean_charisma");
		double stdCharisma = params.getDouble("std_charisma");
		int doctorCount = params.getInteger("doctor_count");
		for (int i = 0; i < doctorCount; i++) {
			Doctor doctor = new Doctor(space, grid, meanCharisma, stdCharisma, r);
			context.add(doctor);
			doctors.add(doctor);
		}
		
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
		
		for (Doctor doctor : doctors) {
			findClosestThreeDoors(doctor, doors, space);
		}
		
		return context;
	}
	
	private void findClosestThreeDoors(Doctor doctor, List<Door> doors, ContinuousSpace<Object> space) {
		double closestDoorDistance = Double.POSITIVE_INFINITY;
		double secondClosestDoorDistance = Double.POSITIVE_INFINITY;
		double thirdClosestDoorDistance = Double.POSITIVE_INFINITY;
		
		NdPoint closestDoor = null,
				secondClosestDoor = null,
				thirdClosestDoor = null;
		
		NdPoint doctorLocation = space.getLocation(doctor);
		
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
