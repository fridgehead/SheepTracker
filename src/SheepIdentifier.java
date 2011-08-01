import java.awt.Point;
import java.util.ArrayList;

import hypermedia.video.Blob;
import hypermedia.video.OpenCV;
import processing.core.PApplet;
import processing.core.PImage;


public class SheepIdentifier {

	SheepTest parent;
	public int sheepSaturationDetection;
	public PImage colorBuffer, removeGreenBuffer;;
	public int colourSampleArea;
	public int maxBlobSize, minBlobSize;
	private OpenCV opencv;
	public int greenThreshLow, greenThreshHigh;
	public ArrayList<Point> sheepList = new ArrayList<Point>();
	public int cameraBlur = 6;
	
	public  SheepIdentifier(SheepTest p){
		this.parent = p;
		opencv = p.opencv;
		removeGreenBuffer = parent.createImage(640,480, parent.RGB);
		colorBuffer = parent.createImage(640,480, parent.RGB);
		
	}
	
	public void update(PImage frame){
		sheepList.clear();
	 // opencv.read();           // grab frame from camera
	  //opencv.copy(colorBuffer);
	  //remember the colour image
	  colorBuffer = frame;
	  //image(colorBuffer,640,0,160,120);


	  //opencv.invert();
	  // opencv.threshold(150, 0, OpenCV.THRESH_OTSU | OpenCV.THRESH_TOZERO);    // set black & white threshold 
	  //opencv.absDiff();                           //  Creates a difference image
	  //opencv.convert(OpenCV.GRAY);                //  Converts to greyscale
	  //opencv.blur(OpenCV.BLUR, 6);                //  Blur to remove camera noise

	  //opencv.threshold(thresh);
	  opencv.blur(OpenCV.BLUR, cameraBlur);
	  removeGreen(opencv.image());
	  //image(removeGreenBuffer, 0,0);
	  opencv.copy(removeGreenBuffer);

	  // find blobs
	  Blob[] blobs = opencv.blobs( minBlobSize, maxBlobSize, 100, false, OpenCV.MAX_VERTICES*4 );
	  removeGreenBuffer.loadPixels();


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


	      
	      if(parent.saturation(avgColor) < sheepSaturationDetection) {      //this blob is a sheep as its colour saturation is low (close to white)
	        /*textFont(myFont);
	        fill(255,0,0);
	        text("Sheep",  blobs[i].centroid.x, blobs[i].centroid.y + 20);*/
	    	  //add this to the sheep list
	    	sheepList.add(blobs[i].centroid );
	    	  
	      }
	      

	      /*fill(avgColor);

	      ellipse(blobs[i].centroid.x, blobs[i].centroid.y,20,20);   //draw the blob with the average colour underneath it
	      fill(0,0,0);*/
	    }
	  }
	  /* handy test for grid of sheep
	  sheepList.clear();
	  for (int x = 0; x < 640; x+= 64){
    	  for (int y = 0; y < 480; y+= 48){
    		  sheepList.add(new Point(x, y));
    	  }
      }*/
	}
	
	public void removeGreen(PImage in) {
		  in.loadPixels();
		  removeGreenBuffer.loadPixels();
		  int c;
		  for(int i = 0; i < 640 * 480; i++) {
		    c =  parent.color((in.pixels[i] >> 16) & 0xFF, (in.pixels[i] >> 8) & 0xFF, (in.pixels[i] ) & 0xFF );

		    if(parent.hue(c) >= greenThreshLow && parent.hue(c) <= greenThreshHigh) {
		      removeGreenBuffer.pixels[i] = 0;
		    } 
		    else {     
		      removeGreenBuffer.pixels[i] = 16777215;
		    }
		  }
		  removeGreenBuffer.updatePixels();

		  // return removeGreenBuffer;
		}
}
