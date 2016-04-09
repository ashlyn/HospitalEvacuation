package bitspls.evacuation;

import bitspls.evacuation.agents.Doctor;
import bitspls.evacuation.agents.GasParticle;
import bitspls.evacuation.agents.Patient;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StickyBorders;

public class HospitalEvacuationBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		context.setId("HospitalEvacuation");

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.StickyBorders(), new double[] {200, 150},
				new double[] {100, 75});

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new StickyBorders(),
						new SimpleGridAdder<Object>(), true, new int[] {200, 150}, new int[] {100, 75}));

		int gasCount = 1;
		for (int i = 0; i < gasCount; i++) {
			context.add(new GasParticle(space, grid));
		}
		
		int doctorCount = 1;
		for (int i = 0; i < doctorCount; i++) {
			context.add(new Doctor(space, grid));
		}
		
		int patientCount = 10;
		for (int i = 0; i < patientCount; i++) {
			context.add(new Patient(space, grid));
		}

		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}

		return context;
	}

}
