import java.awt.Color;

import processing.core.*;

import controlP5.*;
import hypermedia.video.*;

public class SheepTest extends PApplet {



	OpenCV opencv;
	ControlP5 controlP5;

	int thresh = 0;   //threshold value 
	int minBlobSize = 10;
	int maxBlobSize = 0;

	int greenThreshLow = 65;
	int greenThreshHigh = 80;

	int colourSampleArea = 5;  //radius of area to colour sample around blob centroids

	int sheepSaturationDetection = 0;  //saturation threshhold at which we think this is a sheep

	//colour buffer for object recognition
	PImage colorBuffer;
	PFont myFont;


	//test area

	PImage test = createImage(640,480, RGB);
	PImage removeGreenBuffer;


	public void setup() {
	  size (1024,600);
	  colorMode(RGB, 100);
	  opencv = new OpenCV( this );
	  opencv.allocate(640,480);
	 
	  opencv.capture(640,480);

	  //green removal buffer. Allocated once to save time
	  removeGreenBuffer = createImage(640,480, RGB);

	  controlP5 = new ControlP5(this);
	  controlP5.addSlider("thresh",0,255,0,650,260,100,14).setId(4);
	  controlP5.addSlider("minBlobSize",0,255,0,650,280,100,14).setId(1);
	  controlP5.addSlider("maxBlobSize",0,1500,0,650,300,100,14).setId(2);
	  controlP5.addSlider("colourSampleArea",0,30,0,650,320,100,14).setId(5);
	  controlP5.addSlider("sheepSaturationDetection",0,255,0,650,360,100,14).setId(6);
	  controlP5.addSlider("greenThreshLow",0,255,0,650,380,100,14).setId(7);
	  controlP5.addSlider("greenThreshHigh",0,255,0,650,400,100,14).setId(8);
	  controlP5.addBang("rememberbg",650,220, 20,20);

	  maxBlobSize = 10;

	  myFont = loadFont("SansSerif-10.vlw");

	  //test  
	  String[] patts = {
	    "1.pat", "2.pat", "3.pat", "4.pat"
	  };
	  // array of corresponding widths in mm
	  double[] widths = {
	    80,80,80,80
	  };
	  
	  /*
	  // initialise the NyARMultiBoard
	  // the camera parameter file is also in the data subdir
	  nya=new NyARMultiBoard(this,640,480,"camera_para.dat",patts,widths);
	  print(nya.VERSION);
	  nya.gsThreshold=120;
	  nya.cfThreshold=0.4;
	  */
	}

	public void draw() {
	  background(0,0,0);


	  opencv.read();           // grab frame from camera
	  //opencv.copy(colorBuffer);
	  //remember the colour image
	  colorBuffer = opencv.image();
	  image(colorBuffer,640,0,160,120);


	  //opencv.invert();
	  // opencv.threshold(150, 0, OpenCV.THRESH_OTSU | OpenCV.THRESH_TOZERO);    // set black & white threshold 
	  //opencv.absDiff();                           //  Creates a difference image
	  //opencv.convert(OpenCV.GRAY);                //  Converts to greyscale
	  //opencv.blur(OpenCV.BLUR, 6);                //  Blur to remove camera noise

	  //opencv.threshold(thresh);
	  removeGreen(opencv.image());
	  image(removeGreenBuffer, 0,0);
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
	      int avgColor =  color (avgRed,avgGreen,avgBlue);



	      if(saturation(avgColor) < sheepSaturationDetection) {      //this blob is a sheep as its colour saturation is low (close to white)
	        textFont(myFont);
	        fill(255,0,0);
	        text("Sheep",  blobs[i].centroid.x, blobs[i].centroid.y + 20);
	      }

	      fill(avgColor);

	      ellipse(blobs[i].centroid.x, blobs[i].centroid.y,20,20);   //draw the blob with the average colour underneath it
	      fill(0,0,0);
	    }
	  }
	  colorMode(HSB, 360,100,100);
	  fill(greenThreshLow,100,100);
	  rect(630,380,20,20);
	  fill(greenThreshHigh,100,100);
	  rect(630,400,20,20);
	  colorMode(RGB, 255);

	  
	}

	public void removeGreen(PImage in) {
	  in.loadPixels();
	  removeGreenBuffer.loadPixels();
	  int c;
	  for(int i = 0; i < 640 * 480; i++) {
	    c =  color((in.pixels[i] >> 16) & 0xFF, (in.pixels[i] >> 8) & 0xFF, (in.pixels[i] ) & 0xFF );

	    if(hue(c) >= greenThreshLow && hue(c) <= greenThreshHigh) {
	      removeGreenBuffer.pixels[i] = 0;
	    } 
	    else {     
	      removeGreenBuffer.pixels[i] = 16777215;
	    }
	  }
	  removeGreenBuffer.updatePixels();

	  // return removeGreenBuffer;
	}


	//sweep around the centroid looking for a region of colour 
	public boolean isThisACar(Blob blob) {
	  return false;
	}

	public void keyPressed() {
	}

	public void controlEvent(ControlEvent theEvent) {
	  if(theEvent.name().equals("rememberbg") ) {
	    println("bang");
	    opencv.remember();
	  }
	}

  
  public static void main(String args[]) {
	    PApplet.main(new String[] {"SheepTest" });
	  }
  
}