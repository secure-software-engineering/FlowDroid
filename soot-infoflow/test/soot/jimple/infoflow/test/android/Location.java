package soot.jimple.infoflow.test.android;

public class Location {
	
	private double longitude;
	private double latitude;
	
	public Location() {
		
	}
	
	public Location(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	public double getLatitude() {
		return this.latitude;
	}

	public Location clear() {
		this.longitude = 0.0d;
		this.latitude = 0.0d;
		return this;
	}
	
	public Location clearLongitude() {
		this.longitude = 0.0d;
		return new Location();
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

}
