import java.awt.Color;
import java.awt.Point;

import processing.core.*;

import controlP5.*;
import hypermedia.video.*;

public class SheepTest extends PApplet {



	public OpenCV opencv;
	ControlP5 controlP5;
	Textarea logTextArea;
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
	int cameraBlur = 6;

	public int mode = 0;
	public static final int MODE_IDLE = 0;
	public static final int MODE_CONFIG_SHEEP_SPACE = 1;
	public static final int MODE_CONFIG_GPS_SPACE = 2;
	public static final int MODE_RUNNING = 3;

	private boolean useCamera = false;

	PImage bgImage;


	//quad drawing things
	int quadCounter = 0;
	FieldModel.CalibrationQuad sheepCalibQuad;
	Point.Float[] sheepCalibrationPoints = new Point.Float[4];

	int gpsQuadCounter = 0;
	FieldModel.CalibrationQuad gpsCalibQuad;
	Point.Float[] gpsCalibrationPoints = new Point.Float[4];

	int compassCorrection = 0;

	//tcp server
	TankServer tankServer;

	//colour buffer for object recognition
	PImage colorBuffer;
	PFont myFont, niceFont;


	//test area
	PImage test = createImage(640,480, RGB);
	PImage removeGreenBuffer;

	public void setup() {
		size (1480,800);
		colorMode(RGB, 100);
		controlP5 = new ControlP5(this);
		//failed attempt at logging list
		logTextArea = controlP5.addTextarea("Log", "Started logging..\n", 330,620,800,100);
		
		controlP5.addSlider("thresh",0,255,0,			20,160,150,14).setId(4);
		controlP5.addSlider("minBlobSize",0,255,0,		20,180,150,14).setId(1);
		controlP5.addSlider("maxBlobSize",0,1500,0,		20,200,150,14).setId(2);
		controlP5.addSlider("colourSampleArea",0,30,0,	20,220,150,14).setId(5);
		controlP5.addSlider("sheepSaturationDetection",0,255,0,	20,260,150,14).setId(6);
		controlP5.addSlider("greenThreshLow",0,255,0,	20,280,150,14).setId(7);
		controlP5.addSlider("greenThreshHigh",0,255,0,	20,300,150,14).setId(8);
		controlP5.addSlider("cameraBlur",0,255,0,		20,320,150,14).setId(10);
		controlP5.addSlider("compassCorrection",0,360,0,20,340,150,14).setId(9);
		controlP5.addBang("CameraTest",20,350, 20,20);

		//config bangs
		controlP5.addBang("ConfigSheepPerspective",20,400, 20,20);
		controlP5.addBang("IdleMode",20,450, 20,20);
		controlP5.addBang("GpsConfigMode",20,500, 20,20);
		controlP5.addBang("GpsReadPosition",120,500, 20,20);
		controlP5.addBang("RunMode",20,550, 20,20);

		opencv = new OpenCV( this );
		opencv.allocate(640,480);

		//opencv.capture(640,480);
		opencv.movie("data/test.mov");
		bgImage = loadImage("data/sheep.jpg");

		sheepFinder = new SheepIdentifier(this);
		fieldModel = new FieldModel(this);
		sheepCalibrationPoints[0] = new Point.Float(0,0);
		sheepCalibrationPoints[1] = new Point.Float(640,0);
		sheepCalibrationPoints[2] = new Point.Float(640,480);
		sheepCalibrationPoints[3] = new Point.Float(0,480);

		
		
		
		maxBlobSize = 10;

		myFont = loadFont("SansSerif-10.vlw");
		niceFont = loadFont("Arial-Black-48.vlw");
		tankServer = new TankServer(this);
		new Thread(tankServer).start();

	}

	public void tankUpdate(TankServer.TankUpdate update){
		fieldModel.updateTank(update);
	}



	public void draw() {
		background(0,0,0);
		textFont(niceFont);
		fill(0,61,130);
		stroke(0,61,130);
		line(0,730,1280,730);
		text("SheepTracker v0.1", 10, 780);
		textFont(niceFont, 10);
		switch(mode){

		case(MODE_IDLE):
			fill(255,0,0);
		text("Idle", 10, 720);
		break;
		case(MODE_CONFIG_SHEEP_SPACE):
			fill(255,0,0);
		text("Configure Sheep transform", 10, 720);
		break;
		case(MODE_CONFIG_GPS_SPACE):
			fill(255,0,0);
		text("Configure GPS Coords, pos: " + gpsQuadCounter, 10, 720);
		break;			
		}

		text("" + gpsCalibQuad, 10, 680);

		fieldModel.setCompassCorrection(compassCorrection);

		if(useCamera){
			opencv.read();
		} else {
			//opencv.copy(bgImage);
			opencv.loadImage("data/sheep.jpg");
		}
		frame = opencv.image();
		sheepFinder.minBlobSize = minBlobSize;
		sheepFinder.maxBlobSize = maxBlobSize;
		sheepFinder.greenThreshLow = greenThreshLow;
		sheepFinder.greenThreshHigh = greenThreshHigh;
		sheepFinder.colourSampleArea = colourSampleArea;  //radius of area to colour sample around blob centroids
		sheepFinder.sheepSaturationDetection = sheepSaturationDetection;

		image(frame,160,0,160,120);
		sheepFinder.cameraBlur = cameraBlur;
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
		if(mode == MODE_IDLE || mode == MODE_RUNNING){
			//this gives us coordinates in camera space
			for(Point b : sheepFinder.sheepList){
				ellipse(b.x / 4, b.y / 4, 10,10);
			}
			fieldModel.updateSheepPositions(sheepFinder.sheepList);
			fieldModel.draw(new Point(330,0));
		} else if (mode == MODE_CONFIG_SHEEP_SPACE) {
			pushMatrix();
			translate(330,0);
			image(frame,0,0,640,480);
			stroke(255,0,0);

			line((float)sheepCalibrationPoints[0].getX(), (float)sheepCalibrationPoints[0].getY(), (float)sheepCalibrationPoints[1].getX(), (float)sheepCalibrationPoints[1].getY() );
			line((float)sheepCalibrationPoints[1].getX(), (float)sheepCalibrationPoints[1].getY(), (float)sheepCalibrationPoints[2].getX(), (float)sheepCalibrationPoints[2].getY() );

			line((float)sheepCalibrationPoints[2].getX(), (float)sheepCalibrationPoints[2].getY(), (float)sheepCalibrationPoints[3].getX(), (float)sheepCalibrationPoints[3].getY() );
			line((float)sheepCalibrationPoints[3].getX(), (float)sheepCalibrationPoints[3].getY(), (float)sheepCalibrationPoints[0].getX(), (float)sheepCalibrationPoints[0].getY() );
			popMatrix();

		} 


		if(mode == MODE_RUNNING){
			pushMatrix();
			translate(330, 620);
			textFont(niceFont, 20);
			fill(0,255,0);
			text("TANKS LIVE", 0,0);
			popMatrix();
		}
	}


	public void mouseClicked(){
		if(mode == MODE_CONFIG_SHEEP_SPACE){
			if(quadCounter < 3){
				if(mouseX > 330 && mouseX < 330 + 800 && mouseY > 0 && mouseY < 600){
					Point.Float p = new Point.Float(mouseX - 330, mouseY);
					sheepCalibrationPoints[quadCounter] = p;
					quadCounter ++;
				}
			} else {
				Point.Float p = new Point.Float(mouseX - 330, mouseY);
				sheepCalibrationPoints[quadCounter] = p;
				fieldModel.setSheepTransform(sheepCalibrationPoints);

				quadCounter = 0;			

			}
		} else if (mode == MODE_IDLE){
			fieldModel.mouseClicked(mouseX, mouseY);

		}
	}
	
	public void log(String text){
		if(text != null){
			logTextArea.setText(logTextArea.text() + text + "\n");
		}
		System.out.println(text);
	}

	public void keyPressed() {

		//fieldModel.tankController.rotate((int)(Math.random() * 3), -3 + (int)(Math.random() + 4));
		fieldModel.keyPressed(keyCode);
		
	}

	public void controlEvent(ControlEvent theEvent) {
		if(theEvent.name().equals("ConfigSheepPerspective") ) {
			mode = MODE_CONFIG_SHEEP_SPACE;
			quadCounter = 0;

		} else if(theEvent.name().equals("IdleMode") ) {
			mode = MODE_IDLE;

		} else if(theEvent.name().equals("GpsConfigMode") ) {
			mode = MODE_CONFIG_GPS_SPACE;

		} else if(theEvent.name().equals("CameraTest") ) {
			useCamera = !useCamera;

		}  else if(theEvent.name().equals("GpsReadPosition") ) {
			//read tank 1's gps coord
			if(fieldModel.tankList.size() > 0){
				Tank t = fieldModel.tankList.get(0);
				gpsCalibrationPoints[gpsQuadCounter] = t.worldPosition;
				log("calibpt: " + t.worldPosition);
				gpsQuadCounter++;

				if(gpsQuadCounter == 4){
					gpsQuadCounter = 0;
					//set a new gps quad in fieldModel
					fieldModel.setTankTransform(gpsCalibrationPoints);
				}

			}


		}  else if(theEvent.name().equals("RunMode") ) {
			mode = MODE_RUNNING;
		}


	}


	public static void main(String args[]) {
		PApplet.main(new String[] {"SheepTest" });
	}



}