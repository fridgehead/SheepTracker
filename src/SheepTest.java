import java.awt.Color;
import java.awt.Point;

import processing.core.*;

import controlP5.*;
import hypermedia.video.*;

public class SheepTest extends PApplet {



	public OpenCV opencv;
	ControlP5 controlP5;
	PImage frame;
	
	SheepIdentifier sheepFinder;
	FieldModel fieldModel;

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
	  fieldModel = new FieldModel(this);
	  //green removal buffer. Allocated once to save time
	  

	  controlP5 = new ControlP5(this);
	  controlP5.addSlider("thresh",0,255,0,20,160,100,14).setId(4);
	  controlP5.addSlider("minBlobSize",0,255,0,20,180,100,14).setId(1);
	  controlP5.addSlider("maxBlobSize",0,1500,0,20,200,100,14).setId(2);
	  controlP5.addSlider("colourSampleArea",0,30,0,20,220,100,14).setId(5);
	  controlP5.addSlider("sheepSaturationDetection",0,255,0,20,260,100,14).setId(6);
	  controlP5.addSlider("greenThreshLow",0,255,0,20,280,100,14).setId(7);
	  controlP5.addSlider("greenThreshHigh",0,255,0,20,300,100,14).setId(8);
	  //controlP5.addBang("rememberbg",20,220, 20,20);

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
	  
	  image(frame,160,0,160,120);
	  sheepFinder.update(frame);
	  
	  image(sheepFinder.removeGreenBuffer, 0,0,160,120);
	  
	 
	  
	  //feed the camera coords into 
	  fill(255,255,255);
	  textFont(myFont,20);
	  text("Image Control", 10,140);
	  colorMode(HSB, 360,100,100);
	  fill(greenThreshLow,100,100);
	  rect(0,280,20,20);
	  fill(greenThreshHigh,100,100);
	  rect(0,300,20,20);
	  colorMode(RGB, 255);
	  
	  
	  //Draw The fieldModel
	  
	  //this gives us coordinates in camera space
	  for(Blob b : sheepFinder.sheepList){
		  ellipse(b.centroid.x, b.centroid.y, 10,10);
	  }
	  fieldModel.updateSheepPositions(sheepFinder.sheepList);
	  fieldModel.draw(new Point(330,0));
	  
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