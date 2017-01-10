package edu.mit.simile.longwell;

/**
 * A rectangular selection window within a coordinate system.
 * To complicate matters, the coordinate system may be finite and wrap instead
 * of extending to inifinity in all directions.
 * 
 * The origin (0,0) is in the middle.
 * 
 * This is geared towards Google Maps.  Maps may wrap by longitude; if the entire
 * longitudinal range is covered, GMaps will never return anything other than -180 to 180.
 * GMaps does not wrap on the latitudinal axis.  The only wrapping concern is thus a window
 * containing the 180th meridian that does not contain the entire longitudinal range.
 */

public class CoordinateWindow {

    private Coordinates m_sw;
	private Coordinates m_ne;
	private CoordinateWindow m_bounds;
	
	// only set if the coordinate window wraps its boundaries
	private CoordinateWindow m_west;
	private CoordinateWindow m_east;
	
	final static double MAX_LONGITUDE = 180;
	final static double MAX_LATITUDE = 85; // not entirely correct, but GMaps does not cover the whole globe
	final static double MIN_LONGITUDE = -MAX_LONGITUDE;
	final static double MIN_LATITUDE = -MAX_LATITUDE;
	final static CoordinateWindow MAX_LAT_LONG_WINDOW = new CoordinateWindow(new Coordinates(MIN_LATITUDE, MIN_LONGITUDE), new Coordinates(MAX_LATITUDE, MAX_LONGITUDE), null);
	
	public CoordinateWindow(Coordinates sw, Coordinates ne) {
		this(sw, ne, MAX_LAT_LONG_WINDOW);
	}

	public CoordinateWindow(Coordinates sw, Coordinates ne, CoordinateWindow bounds) {
		this.m_sw = sw;
		this.m_ne = ne;
		this.m_bounds = bounds;
		
		if (wraps()) makeComposites();
	}
	
	private void makeComposites() {
		this.m_east = new CoordinateWindow(getSouthWest(), new Coordinates(getNorthEast().getLatitude(), MAX_LONGITUDE), null);
		this.m_west = new CoordinateWindow(new Coordinates(getSouthWest().getLatitude(), MIN_LONGITUDE), getNorthEast(), null);
	}
	
	public Coordinates getSouthWest() {
		return this.m_sw;
	}
	
	public Coordinates getNorthEast() {
		return this.m_ne;
	}
	
	public boolean wraps() {
		return getSouthWest().compareTo(getNorthEast()) > 0;
	}
	
	public double getWidth() {
		return wraps() ? 
				m_west.getWidth() + m_east.getWidth()
				: getNorthEast().getLongitude() - getSouthWest().getLongitude();
	}
	
	public double getHeight() {
		return getNorthEast().getLatitude() - getSouthWest().getLatitude();
	}
	
	public CoordinateWindow getBounds() {
		return this.m_bounds;
	}
	
	public boolean contains(Coordinates c) {
		boolean result = false;
		if (null != c) {
			if (wraps()) {
				if (m_west.contains(c) || m_east.contains(c))
					result = true;
			} else {
				if (getSouthWest().compareTo(c) < 0 && getNorthEast().compareTo(c) > 0)
					result = true;
			}
		}
		return result;
	}
	
	// this really makes no sense as a method; comparison of areas
	public int compareTo(CoordinateWindow w) {
		int result = 0;
		if (!this.equals(w)) {
			result = this.getWidth() *  this.getHeight() < w.getWidth() * w.getHeight() ? -1 : 1;
		}
		return result;
	}
	
	public String toString() {
		return getSouthWest().toString() + " to " + getNorthEast().toString();
	}
}
