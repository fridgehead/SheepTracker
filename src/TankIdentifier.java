import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.media.jai.PerspectiveTransform;



import hypermedia.video.Blob;
import hypermedia.video.OpenCV;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;


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
	private ArrayList<Tank> possibleTanks = new ArrayList<Tank>();
	public ArrayList<Tank> snapShotTanks = new ArrayList<Tank>();
	public ArrayList<Tank> finalTankList = new ArrayList<Tank>();	//final tank list
	private PerspectiveTransform tankTransform;

	private int[][] pairId = new int[3][2];
	private float[][] lastFrameAngles = new float[3][5];
	public float maxTankSize = 10;
	
	private boolean snapshotTaken = false;


/*
 * violet -> red  = 5 -> 0

orange -> blue = 1 -> 4

blue -> red = 4 -> 0


//orange -> green

 */
	public  TankIdentifier(SheepTest p){
		this.parent = p;
		this.opencv = parent.opencv;

		colorBuffer = parent.createImage(640,480, PConstants.RGB);
		tankPointList = new ArrayList<TankPoint>();

		// zero element is the front of the tank
		
		pairId[0][0] = 5;
		pairId[0][1] = 0;
		
		pairId[1][0] = 1;
		pairId[1][1] = 4;
		
		pairId[2][0] = 4;
		pairId[2][1] = 0;
		/*
		pairId[0][0] = 1;
		pairId[0][1] = 3;
		
		pairId[1][0] = 1;
		pairId[1][1] = 4;
		
		pairId[2][0] = 4;
		pairId[2][1] = 0;*/
		
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
		possibleTanks.clear();
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
						TankPoint t = new TankPoint(this);
						t.setColour(new Color(avgRed, avgGreen, avgBlue));
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
		identTanks();
	}

	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setTankTransform(CalibrationQuad quad){
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
		setTankTransform(new CalibrationQuad(p));
	}


	/* take a snapshot of the tanks
	 * store the tank data in tankSnapshot<>
	 * 
	 */
	public void tankSnapShot(){
		snapshotTaken = true;

	}

	/** skim the tank points list and generate pairs of coloured blobs
	 * determine their distance, discard ones that are too large
	 * compare the angles of them (w.r.t. the "field north" ) and discard ones that are drastically different from already
	 * tracked tanks
	 * 
	 */
	private void identTanks(){
		finalTankList.clear();
		possibleTanks.clear();
		for(int i = 0; i < pairId.length; i++){
			//get all points that are part of this pair
			for(TankPoint srcPoint : tankPointList){
				for(TankPoint dstPoint : tankPointList){
					
					if(srcPoint.colourId == pairId[i][0] && dstPoint.colourId == pairId[i][1]){
						//possible pair, check its distance
						float dist = (float) srcPoint.position.distance(dstPoint.position);
						if(dist < maxTankSize){
							//not too far away, check its angles
							
							
							srcPoint.pairId = i;
							dstPoint.pairId = i;
							
							
							PVector srcPos = new PVector(srcPoint.position.x , srcPoint.position.y);
							PVector dstPos = new PVector(dstPoint.position.x , dstPoint.position.y);
							float angle = PVector.angleBetween(new PVector(0,1), PVector.sub(srcPos, dstPos));
							//take this angle and look for tanks that have similar angles.
							
							if(srcPoint.position.x > dstPoint.position.x){
								angle = (float) ((Math.PI * 2 )-angle);
							}
							Point newP = new Point((srcPoint.position.x + dstPoint.position.x )/ 2, (srcPoint.position.y + dstPoint.position.y )/ 2);
							Tank t = new Tank(i, newP, null);
							t.heading = (int) Math.toDegrees(angle);
							//System.out.println("ang: " + angle);
							possibleTanks.add(t);
							
							
						}
					}
					
				}
			}
			
		}
		if(snapshotTaken){
			System.out.println("SNAPSHOT: " );

			snapshotTaken = false;
			snapShotTanks = new ArrayList<Tank>(possibleTanks);
			Collections.copy(snapShotTanks, possibleTanks);
			
		}
		
		//for each possible tank see if its angle is near enough to the snapshot list
		System.out.println("---- tank dump-------pt: " + possibleTanks.size() + " st: " + snapShotTanks.size());
		int ct = 0;
		for(Tank pt : possibleTanks){
			
			for(Tank st : snapShotTanks){
				
				float ang1 = (float)Math.toRadians(pt.heading) ;
				
				float ang2 = (float)Math.toRadians(st.heading) ;
				float angDiff = (float)(normalizeAngle(normalizeAngle(ang2, 0) - normalizeAngle(ang1, 0), 0));
				
				
				System.out.print("possible tank : " + ct + " - tankID " + pt.tankId + " - angdiff = " + angDiff) ;
				if(pt.isTracked == false){
					if(Math.abs(angDiff) > 0.2){
					
						pt.tankId = -1;
						pt.isTracked = false;
						
					
						System.out.println("..discarding");
					} else {
						pt.tankId = ct;
						pt.isTracked = true;
						System.out.println("..keeping");
					}
				} else {
					System.out.println("..is already tracked");
				}
				
			}
			ct++;
		}
		snapShotTanks.clear();
		
		for(Tank pt : possibleTanks){
			if(pt.isTracked){
				//itr.remove();
				finalTankList.add(pt);
				snapShotTanks.add(pt);
			
			} 
		}
	    System.out.println("------- snapshottanks---");
	    for(Tank t : snapShotTanks){
	    	System.out.println("snaptank - id: " + t.tankId + ", heading: " + t.heading   );	    	
	    }
		
		
	}
	public static double normalizeAngle(double a, double center) {
	       return a - (Math.PI * 2) * Math.floor((a + Math.PI - center) / (Math.PI * 2));
	   }
}
