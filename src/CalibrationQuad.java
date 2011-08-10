import java.awt.Point;


public class CalibrationQuad{
	Point.Float p1,p2,p3,p4;
	public CalibrationQuad(Point.Float[] p){
		p1 = p[0];
		p2 = p[1];
		p3 = p[2];
		p4 = p[3];

	}

	public String toString(){
		return "p1: " + p1 + " p2: " + p2 + "\np3: " + p3 + " p4: " + p4;
	}
}