import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import hypermedia.video.Blob;
import javax.media.jai.*;

import processing.core.PImage;



public class FieldModel {
	private SheepTest parent;
	private ArrayList<Point2D> sheepList = new ArrayList<Point2D>();
	PerspectiveTransform sheepTransform;
	PImage sheepImage;

	public FieldModel(SheepTest parent){
		this.parent = parent;
		sheepTransform = new PerspectiveTransform();
		Point[] p = new Point[4];
		p[0] = new Point(0,0);
		p[1] = new Point(640,0);
		p[2] = new Point(640,480);
		p[3] = new Point(0,480);
		setSheepTransform(new CalibrationQuad(p));
		sheepImage = parent.loadImage("sheep.png");
	}
	
	/*
	 * takes a list of sheep blobs, transforms them to field-space and stores them
	 */
	public void updateSheepPositions(ArrayList<Blob> sheepListIn){
		//take each entry and run it through the transformation to field-space
		sheepList.clear();
		for (Blob b : sheepListIn){
			Point2D p = null;
			
			p = sheepTransform.transform(b.centroid, p);
			
			sheepList.add(p);
		}
		
	}
	
	public void setSheepTransform(Point[] p){
		setSheepTransform(new CalibrationQuad(p));
	}
	
	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setSheepTransform(CalibrationQuad quad){
		sheepTransform = new PerspectiveTransform();
		sheepTransform = PerspectiveTransform.getQuadToQuad((float)quad.p1.x, (float)quad.p1.y, 
															(float)quad.p2.x, (float)quad.p2.y, 
															(float)quad.p3.x, (float)quad.p3.y,
															(float)quad.p4.x, (float)quad.p4.y, 
															0.0f, 0.0f, 
															800.0f, 0.0f, 
															800.0f, 600.0f, 
															0.0f, 600.0f);
		
		
		System.out.println(sheepTransform);
	}
	
	
	public ArrayList<Point2D> getSheepList(){
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
		for(Point2D p : sheepList){
			
			//parent.ellipse((float)p.getX(),(float)p.getY(),10,10);
			parent.image(sheepImage, (float)p.getX(), (float)p.getY());
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
