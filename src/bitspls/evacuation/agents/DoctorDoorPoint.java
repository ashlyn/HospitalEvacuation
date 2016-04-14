package bitspls.evacuation.agents;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import repast.simphony.space.continuous.NdPoint;

public class DoctorDoorPoint {
	private NdPoint point;
	private boolean isOvercrowded;
	private boolean isBlocked;
	private double lastVisit;
	
	public DoctorDoorPoint(NdPoint point, boolean isOvercrowded, boolean isBlocked, double lastVisit) {
		this.point = point;
		this.isOvercrowded = isOvercrowded;
		this.isBlocked = isBlocked;
		this.lastVisit = lastVisit;
	}
	
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
	
	@Override
    public int hashCode() {
        return new HashCodeBuilder(23, 113).
            append(point).
            toHashCode();
    }
	
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
