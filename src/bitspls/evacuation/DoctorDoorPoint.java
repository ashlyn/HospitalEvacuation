package bitspls.evacuation;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import repast.simphony.space.continuous.NdPoint;

/**
 * Class to represent a Doctor's knowledge of a door
 * @author Bits Please
 *
 */
public class DoctorDoorPoint {
	private NdPoint point;			// point where the door is located (typically not whole-number grid points)
	private boolean isOvercrowded;  // indicates if a door is overcrowded - can become not-overcrowded over time
	private boolean isBlocked;		// indicates if a door is blocked by gas - if blocked, it will not become unblocked
	private double lastVisit;		// tick count for the last time a doctor visited or observed the door - indicates freshness of data compared to other doctors'
	
	/**
	 * Constructor for a DoctorDoorPoint
	 * @param point Point where a door is located
	 * @param isOvercrowded Is the door overcrowded
	 * @param isBlocked Is the door blocked by gas
	 * @param lastVisit Tick count for last visit
	 */
	public DoctorDoorPoint(NdPoint point, boolean isOvercrowded, boolean isBlocked, double lastVisit) {
		this.point = point;
		this.isOvercrowded = isOvercrowded;
		this.isBlocked = isBlocked;
		this.lastVisit = lastVisit;
	}
	
	/*
	 * Getters & Setters
	 */
	public NdPoint getPoint() {
		return this.point;
	}
	
	public void setPoint(NdPoint point) {
		this.point = point;
	}
	
	public boolean isOvercrowded() {
		return this.isOvercrowded;
	}
	
	public void setOvercrowded(boolean overcrowded) {
		this.isOvercrowded = overcrowded;
	}
	
	public boolean isBlocked() {
		return this.isBlocked;
	}
	
	public void setBlocked(boolean blocked) {
		this.isBlocked = blocked;
	}
	
	public double getLastVisitedTime() {
		return this.lastVisit;
	}
	
	public void setLastVisitedtime(double time) {
		this.lastVisit = time;
	}
	
	/*
	 * Overriding hasCode() and equals() for comparison
	 */
	
	@Override
    public int hashCode() {
        return new HashCodeBuilder(23, 113).
            append(point).
            toHashCode();
    }
	
	/**
	 * Compare two DoctorDoorPoints based on the point's x- and y-values
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DoctorDoorPoint)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		
		DoctorDoorPoint point = (DoctorDoorPoint) obj;
		return point.point.getX() == this.point.getX() && point.point.getY() == this.point.getY();
	}
}
