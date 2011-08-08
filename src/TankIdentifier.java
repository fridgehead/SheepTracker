import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.util.ArrayList;

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
						t.position = test.centroid;
						tankPointList.add(t);

					}

				}
			}
		}
	}

	public class TankPoint {
		public Color colour;
		public Point position;
	}

}
