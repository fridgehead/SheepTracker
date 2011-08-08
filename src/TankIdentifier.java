import java.awt.Point;
import java.util.ArrayList;

import hypermedia.video.Blob;
import hypermedia.video.OpenCV;
import processing.core.PApplet;
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
	public int sheepSaturationDetection;
	public PImage colorBuffer;
	public int colourSampleArea;
	public int maxBlobSize, minBlobSize;

	public  TankIdentifier(SheepTest p){
		this.parent = p;
		
	
		colorBuffer = parent.createImage(640,480, parent.RGB);
	

	}

	public void update(PImage frame){
		
	}
	
}
