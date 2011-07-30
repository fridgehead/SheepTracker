import java.awt.Point;


public class Tank {
	public int heading = -1;
	public float lon = 0.0f;
	public float lat = 0.0f;
	public int accuracy = 0;
	public int type;
	public int tankId;
	
	public Point fieldPosition;
	
	public Tank(int tankId, Point pos){
		this.tankId = tankId;
		this.fieldPosition = pos;
	}
	
	
	
	
}
