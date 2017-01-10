package edu.mit.simile.longwell;

public class Coordinates {
    
	private double m_latitude;
	private double m_longitude;
	
	public Coordinates(double latitude, double longitude) {
		this.m_latitude = latitude;
		this.m_longitude = longitude;
	}
	
	public double getLatitude() {
		return m_latitude;
	}
	
	public double getLongitude() {
		return m_longitude;
	}
	
	public boolean equals(Object c) {
		boolean result = false;
		if (c instanceof Coordinates) {
			if (((Coordinates) c).getLatitude() == this.m_latitude &&
					(((Coordinates) c).getLongitude() == this.m_longitude)) {
				result = true;
			}
		}
		return result;
	}
	
	// South and west are considered 'less than' on a bounded coordinate system
	// North and east are considered 'greater than'
	// any other combination is basically undefined... so 0 means either equal or
	// not strictly less or greater than
	public int compareTo(Coordinates c2) {
		int result = 0;
		
		if (!this.equals(c2)) {
			if (this.compareLatitudeTo(c2) <= 0 && this.compareLongitudeTo(c2) <= 0) {
				result = -1;
			} else if (this.compareLatitudeTo(c2) >= 0 && this.compareLongitudeTo(c2) >= 0) {
				result = 1;
			}
		}
		return result;
	}
	
	public int compareLatitudeTo(Coordinates c2) {
		int result = 0;
		if (this.getLatitude() != c2.getLatitude()) {
			if (this.getLatitude() > c2.getLatitude()) {
				result = 1;
			} else {
				result = -1;
			}
		}
		return result;
	}
	
	public int compareLongitudeTo(Coordinates c2) {
		int result = 0;
		if (this.getLongitude() != c2.getLongitude()) {
			if (this.getLongitude() > c2.getLongitude()) {
				result = 1;
			} else {
				result = -1;
			}
		}
		return result;		
	}
	
	public String toString() {
		return m_latitude + "," + m_longitude;
	}
}
