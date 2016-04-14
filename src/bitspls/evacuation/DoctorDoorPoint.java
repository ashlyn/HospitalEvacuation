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
	private double lastVisit;		// tick count for the last time a doctor visited or observed the door - indicates freshness of data compared to other doctors'
	private DoorPointEnum status;	// status indicating if door is blocked, overcrowded, or available
	
	/**
	 * Constructor for a DoctorDoorPoint
	 * @param point The point on a grid where the door is
	 * @param status The status of the door
	 * @param lastVisit The last time a doctor visited the door
	 */
	public DoctorDoorPoint(NdPoint point, DoorPointEnum status, double lastVisit) {
		this.point = point;
		this.lastVisit = lastVisit;
		this.status = status;
	}

	public DoorPointEnum getStatus() {
		return this.status;
	}
	
	public void setStatus(DoorPointEnum status) {
		this.status = status;
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
