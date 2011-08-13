import java.awt.Color;
import java.awt.Point;

import processing.core.*;

import controlP5.*;
import hypermedia.video.*;

public class SheepTest extends PApplet {



	public OpenCV opencv;
	ControlP5 controlP5;
	ListBox tankList;
	Textarea logTextArea;
	PImage frame;
	Slider threshSlider, minSlider, maxSlider, sampleArea,saturationSlider, hueLowSlider, hueHighSlider;

	int currentEditing = 0;

	SheepIdentifier sheepFinder;
	TankIdentifier tankIdentifier;

	IdentifierSettings[] identSettings = new IdentifierSettings[7];


	public FieldModel fieldModel;
	//tracker params
	public int thresh = 0;   //threshold value 
	int minBlobSize = 10;
	int maxBlobSize = 0;
	int colourThreshLow = 65;
	int colourThreshHigh = 80;
	int colourSampleArea = 5;  //radius of area to colour sample around blob centroids
	int saturationDetection = 0;  //saturation threshhold at which we think this is a sheep
	int cameraBlur = 6;

	int maxTankSize = 10;
	
	public int mode = 0;
	public static final int MODE_IDLE = 0;
	public static final int MODE_CONFIG_SHEEP_SPACE = 1;
	public static final int MODE_CONFIG_GPS_SPACE = 2;
	public static final int MODE_RUNNING = 3;

	private boolean useCamera = false;

	public boolean backgroundSubtractMode = false;
	PImage bgImage;


	//quad drawing things
	int quadCounter = 0;
	CalibrationQuad sheepCalibQuad;
	Point.Float[] sheepCalibrationPoints = new Point.Float[4];

	

	int compassCorrection = 0;


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

		
		threshSlider = controlP5.addSlider("thresh",0,255,0,		20,160,150,14);
		controlP5.addSlider("cameraBlur",0,255,0,					20,180,150,14);
		minSlider = controlP5.addSlider("minBlobSize",0,4000,0,		20,200,150,14);
		maxSlider = controlP5.addSlider("maxBlobSize",0,5500,0,		20,220,150,14);
		sampleArea = controlP5.addSlider("colourSampleArea",1,30,1,	20,240,150,14);
		saturationSlider = controlP5.addSlider("saturationDetection",0,255,0,	20,260,150,14);
		hueLowSlider = controlP5.addSlider("colourThreshLow",0,255,0,	20,280,150,14);
		hueHighSlider = controlP5.addSlider("colourThreshHigh",0,255,0,	20,300,150,14);
		
		controlP5.addSlider("maxTankSize",0,255,0,					20,320,150,14);
		
		controlP5.addBang("CameraTest",20,350, 20,20);
		

		//config bangs
		controlP5.addBang("ConfigSheepPerspective",20,400, 20,20);
		controlP5.addBang("IdleMode",20,450, 20,20);
		controlP5.addBang("RunMode",20,500, 20,20);
		controlP5.addBang("BGSubtractMode",20,590, 20,20);
		controlP5.addBang("remember",120,590, 20,20);
		controlP5.addBang("tankSnapShot",220,590, 20,20);


		tankList = controlP5.addListBox("tankListBox",200,370,120,120);
		tankList.captionLabel().set("Which Thing?");

		tankList.addItem("tank 0 - p1", 0);
		tankList.addItem("tank 0 - p2", 1);
		tankList.addItem("tank 1 - p1", 2);
		tankList.addItem("tank 1 - p2", 3);
		tankList.addItem("tank 2 - p1", 4);
		tankList.addItem("tank 2 - p2", 5);
		tankList.addItem("Sheep", 6);


		for(int i = 0; i < 7; i++){
			identSettings[i] = new IdentifierSettings();
			identSettings[i].colourSampleArea = colourSampleArea;
			identSettings[i].higherHue = colourThreshHigh;
			identSettings[i].lowerHue = colourThreshLow;
			identSettings[i].minBlobSize = minBlobSize;
			identSettings[i].maxBlobSize = maxBlobSize;
			identSettings[i].maxSaturation = saturationDetection;
		}

		opencv = new OpenCV( this );
		opencv.allocate(640,480);

		//opencv.capture(640,480);
		opencv.movie("data/test2.mov");
		bgImage = loadImage("data/sheep.jpg");

		sheepFinder = new SheepIdentifier(this);
		tankIdentifier = new TankIdentifier(this);
		tankIdentifier.setIdentSettings(identSettings);
		fieldModel = new FieldModel(this);
		sheepCalibrationPoints[0] = new Point.Float(0,0);
		sheepCalibrationPoints[1] = new Point.Float(640,0);
		sheepCalibrationPoints[2] = new Point.Float(640,480);
		sheepCalibrationPoints[3] = new Point.Float(0,480);
		tankIdentifier.setTankTransform(sheepCalibrationPoints);




		maxBlobSize = 10;

		myFont = loadFont("SansSerif-10.vlw");
		niceFont = loadFont("Arial-Black-48.vlw");


	}



	public void draw() {
		background(0,0,0);
		textFont(niceFont);
		fill(0,61,130);
		stroke(0,61,130);
		line(0,730,1280,730);
		text("SheepTracker v2", 10, 780);
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
		
		}

	

		fieldModel.setCompassCorrection(compassCorrection);
		tankIdentifier.maxTankSize = maxTankSize;

		if(useCamera){
			opencv.read();
		} else {
			//opencv.copy(bgImage);
			opencv.loadImage("data/sheep.jpg");
		}
		frame = opencv.image();
		sheepFinder.setSettings(identSettings[6]);

		image(frame,160,0,160,120);
		sheepFinder.cameraBlur = cameraBlur;
		sheepFinder.update(frame);
		tankIdentifier.update(sheepFinder.removeGreenBuffer, sheepFinder.colorBuffer);
		
		if(backgroundSubtractMode){
			image(sheepFinder.bgBuffer, 0,0,160,120);

		} else {
			image(sheepFinder.removeGreenBuffer, 0,0,160,120);
		}


		//feed the camera coords into 
		fill(255,255,255);
		textFont(myFont,20);
		text("Image Control", 10,140);
		colorMode(HSB, 360,100,100);
		fill(colourThreshLow,100,100);
		rect(0,280,20,20);
		fill(colourThreshHigh,100,100);
		rect(0,300,20,20);
		colorMode(RGB, 255);


		//Draw The fieldModel
		if(mode == MODE_IDLE || mode == MODE_RUNNING){
			//this gives us coordinates in camera space
			for(Point b : sheepFinder.sheepList){
				ellipse(b.x / 4, b.y / 4, 10,10);
			}
			fieldModel.updateSheepPositions(sheepFinder.sheepList);
			fieldModel.updateTankPositions(tankIdentifier.finalTankList);
			fieldModel.draw(new Point(330,0));
			for(TankPoint t : tankIdentifier.tankPointList){
				fill(t.colour.getRed(), t.colour.getGreen(), t.colour.getBlue());
				ellipse(330 + t.position.x, t.position.y, 10,10);
				textFont(niceFont,15);
				fill(255,255,255);
				text("p: " + t.pairId, 340 + t.position.x, t.position.y);
			}
			
			
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
		System.out.println("mx : " + mouseX + " my: " + mouseY);

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
				tankIdentifier.setTankTransform(sheepCalibrationPoints);

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

		
			
		} else if(theEvent.name().equals("CameraTest") ) {
			useCamera = !useCamera;


		}  else if(theEvent.name().equals("RunMode") ) {
			mode = MODE_RUNNING;
		} else if(theEvent.name().equals("BGSubtractMode") ) {
			backgroundSubtractMode = ! backgroundSubtractMode;
			log("background subtract mode : " + backgroundSubtractMode);
		} else if(theEvent.name().equals("remember") ) {
			sheepFinder.remember();
		} else if(theEvent.name().equals("tankSnapShot") ) {
				tankIdentifier.tankSnapShot();
		} else {
			identSettings[currentEditing] = new IdentifierSettings();
			identSettings[currentEditing].colourSampleArea = colourSampleArea;
			identSettings[currentEditing].higherHue = colourThreshHigh;
			identSettings[currentEditing].lowerHue = colourThreshLow;
			identSettings[currentEditing].minBlobSize = minBlobSize;
			identSettings[currentEditing].maxBlobSize = maxBlobSize;
			identSettings[currentEditing].maxSaturation = saturationDetection;
			tankIdentifier.setIdentSettings(identSettings);
		}


		if (theEvent.isGroup()) {
			// an event from a group e.g. scrollList
			//println(theEvent.group().value()+" from "+theEvent.group());
			currentEditing = (int) theEvent.group().value();


			colourSampleArea = identSettings[currentEditing].colourSampleArea;
			colourThreshHigh = identSettings[currentEditing].higherHue;
			colourThreshLow =identSettings[currentEditing].lowerHue ;
			minBlobSize = identSettings[currentEditing].minBlobSize ;
			maxBlobSize = identSettings[currentEditing].maxBlobSize;
			saturationDetection = identSettings[currentEditing].maxSaturation;
			
			//threshSlider, minSlider, maxSlider, sampleArea,saturationSlider, hueLowSlider, hueHighSlider;
			minSlider.setValue(minBlobSize);
			maxSlider.setValue(maxBlobSize);
			sampleArea.setValue(colourSampleArea);
			saturationSlider.setValue(saturationDetection);
			hueLowSlider.setValue(colourThreshLow);
			hueHighSlider.setValue(colourThreshHigh);
		}

	}


	public static void main(String args[]) {
		PApplet.main(new String[] {"SheepTest" });
	}



}