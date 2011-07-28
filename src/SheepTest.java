import java.awt.Color;

import processing.core.*;

import controlP5.*;
import hypermedia.video.*;

public class SheepTest extends PApplet {



	public OpenCV opencv;
	ControlP5 controlP5;
	PImage frame;
	
	SheepIdentifier sheepFinder;

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
	  size (1280,800);
	  colorMode(RGB, 100);
	  opencv = new OpenCV( this );
	  opencv.allocate(640,480);
	 
	  opencv.capture(640,480);
	  
	  sheepFinder = new SheepIdentifier(this);
	  //green removal buffer. Allocated once to save time
	  

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
	  opencv.read();
	  frame = opencv.image();
	  sheepFinder.minBlobSize = minBlobSize;
	  sheepFinder.maxBlobSize = maxBlobSize;
	  sheepFinder.greenThreshLow = greenThreshLow;
	  sheepFinder.greenThreshHigh = greenThreshHigh;
	  sheepFinder.colourSampleArea = colourSampleArea;  //radius of area to colour sample around blob centroids
	  sheepFinder.sheepSaturationDetection = sheepSaturationDetection;
	  
	  image(frame,640,0,160,120);
	  sheepFinder.update(frame);
	  
	  image(sheepFinder.removeGreenBuffer, 0,0);

	  colorMode(HSB, 360,100,100);
	  fill(greenThreshLow,100,100);
	  rect(630,380,20,20);
	  fill(greenThreshHigh,100,100);
	  rect(630,400,20,20);
	  colorMode(RGB, 255);

	  
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