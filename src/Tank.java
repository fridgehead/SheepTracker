import java.awt.Point;


public class Tank {
	public int heading = -1;
	public float lon = 0.0f;
	public float lat = 0.0f;
	public int accuracy = 0;
	public int type;
	public int tankId;
	
	public Point fieldPosition;
	public Point.Float worldPosition;
	
	
	public Tank(int tankId, Point pos){
		this.tankId = tankId;
		this.fieldPosition = pos;
		worldPosition = new Point.Float (1,1);
	}
	
	public void setPositionFromGPS(float lat, float lon){
		worldPosition.x = (float)(lon*60*1852*Math.cos(lat));
		worldPosition.y  = lat*60*1852;
		
	}
	
	public String toString(){
		return "Tank : " + tankId + ", worldpos: " + worldPosition;
	}
	
	
	
	
}
