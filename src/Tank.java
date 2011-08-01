import java.awt.Point;


public class Tank {
	public int heading = -1;
	public double lon = 0.0;
	public double lat = 0.0;
	public int accuracy = 0;
	public int type;
	public int tankId;
	
	public Point fieldPosition;
	public Point.Float worldPosition;//in x/y not lat/lon
	public Point targetPosition;
	
	public boolean selected = false;
	
	
	public Tank(int tankId, Point pos){
		this.tankId = tankId;
		this.fieldPosition = pos;
		worldPosition = new Point.Float (1,1);
	}
	
	/*
	 * magic numbers are AWESOME
	 */
	public void setPositionFromGPS(double lat, double lon){
		worldPosition.x = (float)(lon*60*1852*Math.cos(lat));
		worldPosition.y  = (float)(lat*60*1852);
		
	}
	
	public String toString(){
		return "Tank : " + tankId + ", worldpos: " + worldPosition + ", fieldPosition: " + fieldPosition + "\n" +
			" lat: " + lat + " lon: "  + lon;
	}
	
	
	
	
}
