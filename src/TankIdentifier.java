import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.media.jai.PerspectiveTransform;



import hypermedia.video.Blob;
import hypermedia.video.OpenCV;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;


/*
 * Class take a pre-cleaned frame and attemps to detect tanks in it
 * 1. look for coloured blobs (with colour settings taken from the parent gui)
 * 2. work out the angle of tank pairs
 * 3. compare these angles to the ones currently stored and reject them if out of range
 * 4. 
 * 
 */
public class TankIdentifier {

	SheepTest parent;
	public PImage colorBuffer;

	private IdentifierSettings[] identSettings = new IdentifierSettings[7];
	private OpenCV opencv;
	public ArrayList<TankPoint> tankPointList;	//temp arraylist of tanks. 
	private PerspectiveTransform tankTransform;
	


	public  TankIdentifier(SheepTest p){
		this.parent = p;
		this.opencv = parent.opencv;

		colorBuffer = parent.createImage(640,480, PConstants.RGB);
		tankPointList = new ArrayList<TankPoint>();

	}

	/* first 3 are assumed to be tanks
	 * 
	 */
	public void setIdentSettings(IdentifierSettings[] i){
		identSettings = i;

	}

	public void update(PImage blobframe, PImage colourFrame){
		colorBuffer = colourFrame;
		opencv.copy(blobframe);
		colorBuffer.loadPixels();
		tankPointList.clear();
		int identCount = 0;
		for(IdentifierSettings b : identSettings){

			int minBlobSize = b.minBlobSize;
			int maxBlobSize = b.maxBlobSize;
			int colourSampleArea = b.colourSampleArea;
			float maxSaturationDetection = b.maxSaturation;


			Blob[] blobs = opencv.blobs( minBlobSize, maxBlobSize, 100, false, OpenCV.MAX_VERTICES*4 );

			for( int i=0; i < blobs.length; i++) {
				//for each blob work out the average colour under it

				int pixelCount = 0 ;  //count of how many pixels we examine
				int avgRed = 0;
				int avgGreen = 0;
				int avgBlue = 0;

				Blob test = blobs[i];
				if(test.area > minBlobSize || test.area > maxBlobSize) {
					for(int px = -colourSampleArea; px < colourSampleArea; px++) {
						for(int py = -colourSampleArea; py < colourSampleArea; py++) {      
							if(test.centroid.x + px < 640 && test.centroid.x + px > 0 && test.centroid.y + py < 480 && test.centroid.y + py > 0) {
								pixelCount ++;

								avgRed +=   ( colorBuffer.pixels[(test.centroid.x + px)  + (test.centroid.y + py) * 640    ] >> 16) & 0xFF;
								avgGreen += ( colorBuffer.pixels[(test.centroid.x + px)  + (test.centroid.y + py) * 640    ] >> 8) & 0xFF;
								avgBlue +=  ( colorBuffer.pixels[(test.centroid.x + px)  + (test.centroid.y + py) * 640    ] ) & 0xFF;
							}
						}
					}
					avgRed /= pixelCount;
					avgGreen /= pixelCount;
					avgBlue /= pixelCount;
					int avgColor =  parent.color (avgRed,avgGreen,avgBlue);

					if(parent.hue(avgColor) >= b.lowerHue && parent.hue(avgColor) <= b.higherHue && parent.saturation(avgColor) > maxSaturationDetection){
						TankPoint t = new TankPoint();
						t.colour = new Color(avgRed, avgGreen, avgBlue);
						//convert coords to field-space
						Point2D p = null;

						p = tankTransform.transform(test.centroid, p);
						t.position = new Point((int)p.getX(), (int)p.getY());
						t.pointId = identCount; 
						tankPointList.add(t);

					}

				}
			}
			identCount++;
		}
	}
	
	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setTankTransform(FieldModel.CalibrationQuad quad){
		tankTransform = new PerspectiveTransform();
		tankTransform = PerspectiveTransform.getQuadToQuad(quad.p1.x, quad.p1.y, 
				quad.p2.x, quad.p2.y, 
				quad.p3.x, quad.p3.y,
				quad.p4.x, quad.p4.y, 
				0.0f, 0.0f, 
				800.0f, 0.0f, 
				800.0f, 600.0f, 
				0.0f, 600.0f);


		System.out.println("TankTransform: " + tankTransform);
	}
	public void setTankTransform(Point.Float[] p){
		setTankTransform(new FieldModel.CalibrationQuad(p));
	}

	
	/* take a snapshot of the tanks
	 * 
	 */
	public void tankSnapShot(){
		
	}
	
	/** skim the tank points list and generate pairs of coloured blobs
	 * determine their distance, discard ones that are too large
	 * compare the angles of them (w.r.t. the "field north" ) and discard ones that are drastically different from already
	 * tracked tanks
	 * 
	 */
	private void identTanks(){
		for (TankPoint src : tankPointList){
			for (TankPoint dst : tankPointList){
				if(src != dst){
					//compare the world distances of them
					
					
					
				}
			}
		}
	}

	public class TankPoint {
		public Color colour;
		public Point position;
		public int pointId = 0;
	}

}
