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
	public FieldModel fieldModel;
	//tracker params
	int thresh = 0;   //threshold value 
	int minBlobSize = 10;
	int maxBlobSize = 0;
	int greenThreshLow = 65;
	int greenThreshHigh = 80;
	int colourSampleArea = 5;  //radius of area to colour sample around blob centroids
	int sheepSaturationDetection = 0;  //saturation threshhold at which we think this is a sheep

	public int mode = 0;
	public static final int MODE_IDLE = 0;
	public static final int MODE_CONFIG_SHEEP_SPACE = 1;

	//quad drawing things
	int quadCounter = 0;
	FieldModel.CalibrationQuad sheepCalibQuad;
	Point[] sheepCalibrationPoints = new Point[4];
	
	//tcp server
	TankServer tankServer;

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
		sheepCalibrationPoints[0] = new Point(0,0);
		sheepCalibrationPoints[1] = new Point(640,0);
		sheepCalibrationPoints[2] = new Point(640,480);
		sheepCalibrationPoints[3] = new Point(0,480);

		controlP5 = new ControlP5(this);
		controlP5.addSlider("thresh",0,255,0,20,160,100,14).setId(4);
		controlP5.addSlider("minBlobSize",0,255,0,20,180,100,14).setId(1);
		controlP5.addSlider("maxBlobSize",0,1500,0,20,200,100,14).setId(2);
		controlP5.addSlider("colourSampleArea",0,30,0,20,220,100,14).setId(5);
		controlP5.addSlider("sheepSaturationDetection",0,255,0,20,260,100,14).setId(6);
		controlP5.addSlider("greenThreshLow",0,255,0,20,280,100,14).setId(7);
		controlP5.addSlider("greenThreshHigh",0,255,0,20,300,100,14).setId(8);
		//config bangs
		controlP5.addBang("ConfigSheepPerspective",20,400, 20,20);
		controlP5.addBang("IdleMode",20,450, 20,20);


		maxBlobSize = 10;

		myFont = loadFont("SansSerif-10.vlw");
		tankServer = new TankServer(this);
		new Thread(tankServer).start();
		
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
		if(mode != MODE_CONFIG_SHEEP_SPACE){
			//this gives us coordinates in camera space
			for(Point b : sheepFinder.sheepList){
				ellipse(b.x, b.y, 10,10);
			}
			fieldModel.updateSheepPositions(sheepFinder.sheepList);
			fieldModel.draw(new Point(330,0));
		} else {
			pushMatrix();
			translate(330,0);
			image(frame,0,0,800,600);
			stroke(255,0,0);
			
			line((float)sheepCalibrationPoints[0].getX(), (float)sheepCalibrationPoints[0].getY(), (float)sheepCalibrationPoints[1].getX(), (float)sheepCalibrationPoints[1].getY() );
			line((float)sheepCalibrationPoints[1].getX(), (float)sheepCalibrationPoints[1].getY(), (float)sheepCalibrationPoints[2].getX(), (float)sheepCalibrationPoints[2].getY() );

			line((float)sheepCalibrationPoints[2].getX(), (float)sheepCalibrationPoints[2].getY(), (float)sheepCalibrationPoints[3].getX(), (float)sheepCalibrationPoints[3].getY() );
			line((float)sheepCalibrationPoints[3].getX(), (float)sheepCalibrationPoints[3].getY(), (float)sheepCalibrationPoints[0].getX(), (float)sheepCalibrationPoints[0].getY() );
			popMatrix();
			
		}

	}


	public void mouseClicked(){
		if(mode == MODE_CONFIG_SHEEP_SPACE){
			if(quadCounter < 3){
				if(mouseX > 330 && mouseX < 330 + 800 && mouseY > 0 && mouseY < 600){
					Point p = new Point(mouseX - 330, mouseY);
					sheepCalibrationPoints[quadCounter] = p;
					quadCounter ++;
				}
			} else {
				Point p = new Point(mouseX - 330, mouseY);
				sheepCalibrationPoints[quadCounter] = p;
				fieldModel.setSheepTransform(sheepCalibrationPoints);

				quadCounter = 0;			

			}
		}
	}

	public void keyPressed() {
	}

	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.name().equals("ConfigSheepPerspective") ) {
			mode = MODE_CONFIG_SHEEP_SPACE;
			quadCounter = 0;

		} else if(theEvent.name().equals("IdleMode") ) {
			mode = MODE_IDLE;

		}
	}


	public static void main(String args[]) {
		PApplet.main(new String[] {"SheepTest" });
	}

}