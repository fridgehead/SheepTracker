import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import processing.core.PVector;


public class Tank {
	public int heading = -1;
	public double lon = 0.0;
	public double lat = 0.0;
	public int accuracy = 0;
	public int type;
	public int tankId;

	public Point fieldPosition;
	public Point.Float worldPosition;//in x/y not lat/lon
	public Point targetPosition;

	public boolean selected = false;
	public float desiredAngle = 0.0f;

	FieldModel parent;
	PVector loc;

	public PVector currentTarget;	//in field-space
	public TankController.TankCommand command = TankController.TankCommand.IDLE;
	


	public Tank(int tankId, Point pos, FieldModel parent){
		this.tankId = tankId;
		this.fieldPosition = pos;
		this.parent = parent;
		worldPosition = new Point.Float (1,1);

		heading = 0;
		currentTarget = new PVector(0,0);
	}


	public String toString(){
		return "Tank : " + tankId + ", worldpos: " + worldPosition + ", fieldPosition: " + fieldPosition + "\n" +
		" lat: " + lat + " lon: "  + lon;
	}



	public  void run(ArrayList<Tank> otherTanks, ArrayList<Point2D> sheep) {
		loc = new PVector(fieldPosition.x, fieldPosition.y);
		currentTarget = target(otherTanks, sheep);
		steer();
	}



	void steer() {

		PVector route = currentTarget.get();
		route.sub(loc);


		 desiredAngle = (float) (route.heading2D() - Math.PI / 2);
		

		float diff = (float) (desiredAngle - Math.toRadians(heading - 90) ) ;
		System.out.println("diff1: " + diff);

		

		if(diff > Math.PI/10 && diff < Math.PI) { 

			//send a command to turn left
			//cancel this command when the angle is near enough
			if(command != TankController.TankCommand.ROTATE_RIGHT){
				parent.tankController.rotate(tankId, 1);
				command = TankController.TankCommand.ROTATE_RIGHT;
			}
		} 
		if(diff < (2*Math.PI) -Math.PI/10 && diff > Math.PI) {


			// INSERT SERIAL 'MOVE RIGHT CODE HERE'
			// or in simulation, uncomment the following
			if(command != TankController.TankCommand.ROTATE_LEFT){
				parent.tankController.rotate(tankId, -1);
				command = TankController.TankCommand.ROTATE_LEFT;
			}


		} 
		if((diff > (2*Math.PI) -Math.PI/10 && diff < 2*Math.PI) || (diff < Math.PI/10 ) && diff > 0) {

			// move forward;

			// INSERT SERIAL 'MOVE FORWARD CODE HERE'
			// or in simulation, uncomment the following
			if(command != TankController.TankCommand.FORWARD){
				parent.tankController.stopRotate(tankId);
				parent.tankController.go(tankId, 1);

				command = TankController.TankCommand.FORWARD;
			}


		}

	}

	// We accumulate a new target each time 
	PVector target(ArrayList<Tank> predators, ArrayList<Point2D> sheep) {

		PVector flockAttract = new PVector (parent.flockCenter.x, parent.flockCenter.y); // use when not in simulation
		PVector goalRepel = repel(); // don't get too close to sheep  

		// total of mult() factors must equal 1
		flockAttract.mult(0.7f);
		goalRepel.mult(0.3f);

		PVector total = new PVector(0,0);
		total.add(flockAttract);
		total.add(goalRepel);



		return total;

	}



	void borders() {
		if (loc.x < 0) loc.x = 0; // width+r;
		if (loc.y < 0) loc.y = 0; // height+r;
		if (loc.x > parent.width) loc.x = parent.width; // -r;
		if (loc.y > parent.height) loc.y = parent.height; // -r;
	}

	PVector attractToCentre(PVector flockCentre) {

		return flockCentre;

	}



	PVector repel() {
		float repelFactor = -0.5f;

		PVector penTemp = new PVector(parent.penLocation.x,parent.penLocation.y);

		PVector result = loc.get();
		result.sub(penTemp);
		result.div(2);
		result.add(loc);


		return result;
	}


}
