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

		pairId[0][0] = 4;
		pairId[0][1] = 0;

		pairId[1][0] = 1;
		pairId[1][1] = 4;

		pairId[2][0] = 5;
		pairId[2][1] = 4;
		/*
		pairId[0][0] = 1;
		pairId[0][1] = 3;

		pairId[1][0] = 1;
		pairId[1][1] = 4;

		pairId[2][0] = 4;
		pairId[2][1] = 0;*/
		colours[0] = 0xFF0000; 
		names[0] =  "Red";

		colours[1] = 0xFFA500; 
		names[1] = "Orange";
		colours[2] = 0xFFFF00; 
		names[2] = "Yellow";
		colours[3] = 0x008000; 
		names[3] = "Green";
		colours[4] = 0x0000FF; 
		names[4] = "Blue";
		colours[5] = 0xEE82EE; 
		names[5] = "Violet";
		//colours[6] = 0xA52A2A; 
		//names[6] = "Brown";
		colours[6] = 0x000000; 
		names[6] = "Black";
		colours[7] = 0x808080; 
		names[7] = "Grey";
		colours[8] = 0xFFFFFF; 
		names[8] = "White";

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


				Blob test = blobs[i];
		        Point[] pts = findColourBlobs(colorBuffer.pixels, test.rectangle);
		        int ct = 0;
		        for(Point p : pts){
		        	if(p.x != -1){
		        		TankPoint t = new TankPoint(this);
						t.setColour(new Color((colours[ct] >> 16) & 0xFF, (colours[ct] >> 8) & 0xFF, (colours[ct] ) & 0xFF));
						//convert coords to field-space
						Point2D pt = null;

						pt = tankTransform.transform(p, pt);
						t.position = new Point((int)pt.getX(), (int)pt.getY());
						t.pointId = identCount; 
						tankPointList.add(t);
		        		
		        		// color c = color((colours[ct] >> 16) & 0xFF, (colours[ct] >> 8) & 0xFF, (colours[ct] ) & 0xFF);
		        	}
		        	ct++;
		        }

				



			}
			//}
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
		//System.out.println("---- tank dump-------pt: " + possibleTanks.size() + " st: " + snapShotTanks.size());
		int ct = 0;
		for(Tank pt : possibleTanks){

			for(Tank st : snapShotTanks){

				float ang1 = (float)Math.toRadians(pt.heading) ;

				float ang2 = (float)Math.toRadians(st.heading) ;
				float angDiff = (float)(normalizeAngle(normalizeAngle(ang2, 0) - normalizeAngle(ang1, 0), 0));


				//System.out.print("possible tank : " + ct + " - tankID " + pt.tankId + " - angdiff = " + angDiff) ;
				if(pt.isTracked == false){
					if(Math.abs(angDiff) > 0.3){

						pt.tankId = -1;
						pt.isTracked = false;


						//System.out.println("..discarding");
					} else {
						pt.tankId = ct;
						pt.isTracked = true;
						//System.out.println("..keeping");
					}
				} else {
					//System.out.println("..is already tracked");
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
		//System.out.println("------- snapshottanks---");
		//for(Tank t : snapShotTanks){
		//	System.out.println("snaptank - id: " + t.tankId + ", heading: " + t.heading   );	    	
		//}


	}
	public static double normalizeAngle(double a, double center) {
		return a - (Math.PI * 2) * Math.floor((a + Math.PI - center) / (Math.PI * 2));
	}
	
	


	private Point[] findColourBlobs(int[] in, Rectangle area){
		int[] colourBinsX = new int[names.length];
		int[] total = new int[names.length];
		int[] colourBinsY = new int[names.length];  
		Point[] ret = new Point[names.length];

		for(int sx = area.x; sx < area.x + area.width; sx++){
			for(int sy = area.y; sy < area.y + area.height; sy++){
				int index = toNameIndex(in[sx + sy * 640]);
				if(index >=0 && index <=5 && index != 3){
					colourBinsX[index] += sx;
					colourBinsY[index] += sy;
					total[index]++;
				}
			}
		}

		for(int i = 0; i < names.length; i++){
			if(total[i] > 0){
				int xp = colourBinsX[i] / total[i];
				int yp = colourBinsY[i] / total[i];
				ret[i] = new Point(xp,yp);
			} else {
				ret[i] = new Point(-1,-1);
			}
		}
		return ret;

	}

	private int toNameIndex(int c) {


		int r = (c >> 16) & 0xFF;
		int g = (c >> 8) & 0xFF;
		int b = (c) & 0xFF;

		HSLColor hsl = new HSLColor(new Color(r,g,b));
		int h = (int)(hsl.getHue() * 0.708333333333333);
		int s = (int)(hsl.getSaturation() * 2.55);
		int l = (int)(hsl.getLuminance() * 2.55);
		float ndf1 = 0; 
		float ndf2 = 0; 
		float ndf = 0;
		int cl = -1;
		float df = -1;

		for(int i = 0; i < names.length; i++)
		{

			ndf1 = (float)(Math.pow(r - parent.red(colours[i]), 2) + Math.pow(g - parent.green(colours[i]), 2) + Math.pow(b - parent.blue(colours[i]), 2));
			ndf2 = (float)(Math.abs(Math.pow(h - parent.hue(colours[i]), 2)) + Math.pow(s - parent.saturation(colours[i]), 2) + Math.abs(Math.pow(l - parent.brightness(colours[i]), 2)));
			ndf = ndf1 + ndf2 * 2;
			if(df < 0 || df > ndf)
			{
				df = ndf;
				cl = i;
			}
		}

		if(cl != -1){

			return cl;
		} else {
			return -1;
		}


	}



}
