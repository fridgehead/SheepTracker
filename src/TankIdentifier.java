import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
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
	private String[] names = new String[9];
	private int[] colours = new int[9];
	private PImage[] testbuf = new PImage[3];

	PImage buf;
	public ArrayList<ColourPoint> trackList = new ArrayList<ColourPoint>();

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

		for (int i = 0; i < 3; i++){
			testbuf[i] = parent.createImage(640,480,PApplet.RGB);
			testbuf[i].loadPixels();
		}

	}

	/* first 3 are assumed to be tanks
	 * 
	 */
	public void setIdentSettings(IdentifierSettings[] i){
		identSettings = i;

	}

	//for each blob, scan it for coloured areas. Identify from these
	//the current blob tracker is looking for the markers, I want to change it to look
	//for tanks then read each tank for a marker
	public void update(PImage blobframe, PImage colourFrame){
		colourFrame.loadPixels();
		trackList.clear();
		finalTankList.clear();
		//opencv.threshold(100, 0, OpenCV.THRESH_OTSU | OpenCV.THRESH_TOZERO);
		Blob[] blobs = opencv.blobs( identSettings[0].minBlobSize, identSettings[0].maxBlobSize, 100, false, OpenCV.MAX_VERTICES*4 );
		for( int i=0; i<blobs.length; i++ ) {

			Rectangle r = blobs[i].rectangle;
			//parent.stroke(0,0,255);
			//parent.rect(r.x,r.y,r.width,r.height);
			ArrayList<ColourPoint> pts = findColourBlobs(colourFrame.pixels, r);
			
			
			trackList.addAll(pts);

		}
		

		identTanks(colourFrame.pixels, trackList);
	}

	ArrayList<ColourPoint> findColourBlobs(int[] in, Rectangle area){
		ArrayList<ColourPoint> retList = new ArrayList<ColourPoint>();  

		int hueThresh = 40;
		int[] hueList = new int[3];
		hueList[0] = 0; //red
		hueList[1] = 150; // blue
		hueList[2] = 220; //purple

		for(int sx = area.x; sx < area.x + area.width; sx++){
			for(int sy = area.y; sy < area.y + area.height; sy++){
				int col = in[sx + sy * 640];
				int c = parent.color((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF);
				int thisHue = (int)parent.hue(c);//(int)(0.708333333333333 * hue(c));
				for(int h = 0; h < hueList.length; h++){

					if( thisHue  > (hueList[h] - hueThresh )   && thisHue < (hueList[h] + hueThresh )  && parent.saturation(c) > 30){

						testbuf[h].pixels[sx + sy * 640] = c;


					} else {
						testbuf[h].pixels[sx + sy * 640] = 0;
					}



				}

			}
		}


		for(int i = 0; i < 3; i++){
			testbuf[i].updatePixels();
			//opencv.remember();
			opencv.copy(testbuf[i]);
			opencv.ROI(area);
			Blob[] blobs = opencv.blobs( 2, 4000, 100, false, OpenCV.MAX_VERTICES*4 );
			for(Blob b :blobs){
				ColourPoint l = new ColourPoint();
				l.colourId = i;
				l.colour = in[b.centroid.x + b.centroid.y * 640];
				l.pos = b.centroid;
				retList.add(l);
			}
		}
		opencv.ROI(0,0,640,480);

		return retList;
	}


	void identTanks(int[] imageIn, ArrayList<ColourPoint> blobs){
		//draw a line from red to blue blobs
		//see if there is a blob nearby that is purple

		for(ColourPoint srcBlob : blobs){  
			for(ColourPoint dstBlob : blobs){    
				if(srcBlob != dstBlob && srcBlob.colourId == 0 && dstBlob.colourId == 2){

					//distance check
					if(srcBlob.pos.distance(dstBlob.pos) < 50){



						//halfway between these points should be a blue blob
						int halfX = 0;

						halfX = (srcBlob.pos.x + dstBlob.pos.x) / 2;



						int halfY = 0;

						halfY = (srcBlob.pos.y + dstBlob.pos.y) / 2;

						Point halfPos = new Point(halfX, halfY);
						for(ColourPoint bluePoint : blobs){
							if(bluePoint.colourId == 1){
								if(bluePoint.pos.distance(halfPos) < 10){

									PVector src = new PVector(srcBlob.pos.x, srcBlob.pos.y);
									PVector dst = new PVector(dstBlob.pos.x, dstBlob.pos.y);

									PVector dir = PVector.sub(src,dst);
									float angle = PVector.angleBetween(new PVector(0,1), dir);

									PVector halfP = new PVector(halfPos.x, halfPos.y);
									PVector normal = new PVector(0,0);
									normal.x += ( PVector.sub(halfP, src)).y;
									normal.y += -( PVector.sub(halfP, src)).x;
									normal.mult(4);

									//from each of the 3 points work backward from the normal vector and read the BW value
									normal.normalize();
									normal.mult(-5);


									int id = 0;
									int col = imageIn[(int)(src.x + normal.x)+(int)(src.y+ normal.y) * 640];
									col = parent.color((col >> 16 ) & 0xFF ,(col >> 8 ) & 0xFF ,(col ) & 0xFF) ;
									if(parent.brightness(col) > 100){
										id |= 1;
										parent.fill(255,255,255);
									} else {
										parent.fill(0);
									}

									parent.rect( halfP.x, halfP.y+30, 10,10);



									col = imageIn[(int)(halfP.x + normal.x)+ (int)(halfP.y+ normal.y) * 640];
									col = parent.color((col >> 16 ) & 0xFF ,(col >> 8 ) & 0xFF ,(col ) & 0xFF) ;
									if(parent.brightness(col) > 100){
										id |= 2;
										parent.fill(255,255,255);
									} else {
										parent.fill(0);
									}

									parent.rect( halfP.x+10, halfP.y+ 30, 10,10);

									col = imageIn[(int)(dst.x + normal.x)+ (int)(dst.y+ normal.y) * 640];
									col = parent.color((col >> 16 ) & 0xFF ,(col >> 8 ) & 0xFF ,(col ) & 0xFF) ;
									if(parent.brightness(col) > 100){
										id |= 4;
										parent.fill(255,255,255);
									} else {
										parent.fill(0);
									}

									parent.rect( halfP.x+20, halfP.y + 30, 10,10);

									//work out angle
									if(id == 1 || id == 5 || id == 6){
										Tank t = new Tank(id,new Point((int)halfP.x, (int)halfP.y), null);
										t.heading = (int) parent.degrees(angle) + 90;
										finalTankList.add(t);
									}
									

								}
							}
						}

					}


				}
			}
		}
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


	public static double normalizeAngle(double a, double center) {
		return a - (Math.PI * 2) * Math.floor((a + Math.PI - center) / (Math.PI * 2));
	}






	public class ColourPoint {
		public int colourId = 0;
		public int colour = 0;
		public Point pos;
		public Point translatedPos;


	}
}
