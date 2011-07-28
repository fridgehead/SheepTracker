import java.awt.Point;
import java.util.ArrayList;

import hypermedia.video.Blob;
import javax.media.jai.*;



public class FieldModel {
	private SheepTest parent;
	private ArrayList<Point> sheepList = new ArrayList<Point>();
	PerspectiveTransform sheepTransform;

	public FieldModel(SheepTest parent){
		this.parent = parent;
		sheepTransform = new PerspectiveTransform();
		Point[] p = new Point[4];
		p[0] = new Point(0,0);
		p[1] = new Point(640,0);
		p[2] = new Point(640,480);
		p[3] = new Point(0,480);
		setSheepTransform(new CalibrationQuad(p));
	}
	
	/*
	 * takes a list of sheep blobs, transforms them to field-space and stores them
	 */
	public void updateSheepPositions(ArrayList<Blob> sheepListIn){
		//take each entry and run it through the transformation to field-space
		sheepList.clear();
		for (Blob b : sheepListIn){
			Point p = new Point(0,0);
			sheepTransform.transform(b.centroid, p);
			sheepList.add(p);
		}
	}
	
	/*
	 * Maps all sheep coordinates from quad space to field space
	 */
	public void setSheepTransform(CalibrationQuad quad){
		sheepTransform = PerspectiveTransform.getQuadToQuad(quad.p1.x, quad.p1.y, 
															quad.p2.x, quad.p2.y, 
															quad.p3.x, quad.p3.y,
															quad.p3.x, quad.p3.y, 
															0, 0, 
															800, 0, 
															800, 600, 
															0, 600);
	}
	
	
	public ArrayList<Point> getSheepList(){
		return sheepList;
	}
	
	public void draw(Point basePos){
		parent.pushMatrix();
		parent.translate(basePos.x, basePos.y);
		parent.pushStyle();
		parent.stroke(255,255,255);
		parent.fill(0,150,0);
		parent.rect(0,0,800,600);
		parent.fill(255,255,255);
		for(Point p : sheepList){
			parent.ellipse(p.x,p.y,10,10);
			
		}
		
		parent.popStyle();
		parent.popMatrix();
		
		
	}
	
	public class CalibrationQuad{
		Point p1,p2,p3,p4;
		public CalibrationQuad(Point[] p){
			p1 = p[0];
			p2 = p[1];
			p3 = p[2];
			p4 = p[3];
			
		}
	}

}
