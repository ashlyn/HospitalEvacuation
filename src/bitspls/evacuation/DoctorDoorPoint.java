package bitspls.evacuation;


import org.apache.commons.lang3.builder.HashCodeBuilder;

import repast.simphony.space.continuous.NdPoint;

public class DoctorDoorPoint {
	private NdPoint point;
	private double lastVisit;
	private DoorPointEnum status;
	
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
