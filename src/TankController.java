import java.util.Vector;

import processing.serial.*;


public class TankController extends Thread {

	SheepTest parent;
	Object lock = new Object();

	private boolean running;
	Serial serial;

	// list of
	Vector<TankCommand> tankCommandList = new Vector<TankCommand>(5);

	public TankController (SheepTest parent, String serialPort){
		this.parent = parent;
		String[] list = Serial.list();
		for(String l : list){
			System.out.println(l);
		}
		int serIndex = -1;
		for(int i = 0; i < list.length; i++){
			if(list.equals(serialPort)){
				serIndex = i;
			}
		}

		if(serIndex == -1){
			parent.log("!!!!!! -- NO SERIAL PORT FOUND!!!!!!!");

		} else {
			serial = new Serial(parent, Serial.list()[serIndex], 9600);
			serial.write("HELLO");
		}

		for(int i = 0; i < 3; i++){
			tankCommandList.add( TankCommand.IDLE);
		}



	}

	private void sendString(int tankId, String in){
		// serial.write(in);
		parent.log("SERIAL COMMAND: $" + tankId + in);
	}

	public void addTank(int index){
		tankCommandList.set(index, TankCommand.IDLE);
		tankCommandList.get(index).processed = false;
	}

	public void start(){
		System.out.println("TankController: started");
		running = true;

		super.start();

	}

	public void end(){
		running = false;
		try {
			join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/* 
	 * rotates a tank in a direction
	 */
	public void rotate(int tankId, int direction){

		if(direction < 0){
			tankCommandList.set(tankId, TankCommand.ROTATE_LEFT);		
			tankCommandList.get(tankId).processed = false;


		} else {
			tankCommandList.set(tankId, TankCommand.ROTATE_RIGHT);
			tankCommandList.get(tankId).processed = false;


		}


	}

	/* stops rotating
	 * 
	 */
	public void stopRotate(int tankId){
		tankCommandList.set(tankId, TankCommand.STOP_ROTATING);
		tankCommandList.get(tankId).processed = false;

	}
	
	
	public void stopMoving(int tankId){
		tankCommandList.set(tankId, TankCommand.STOP_MOVING);
		tankCommandList.get(tankId).processed = false;

	}

	public void go(int tankId, int direction){
		if(direction < 0){
			tankCommandList.set(tankId, TankCommand.BACKWARD);
			tankCommandList.get(tankId).processed = false;
		} else {
			tankCommandList.set(tankId, TankCommand.FORWARD);
			tankCommandList.get(tankId).processed = false;
		}


	}

	public void run(){
		System.out.println("TankController: started thread");

		while(running){

			//read message queue, 


			for(int ind = 0; ind < tankCommandList.size(); ind ++){
				TankCommand tc = tankCommandList.get(ind);			

				if(tc.processed == false){
					if(tc != TankCommand.IDLE){
						sendString(ind, tc.sendString);
						tc.processed = true;
					}

				}
			}


		}
	}

	public enum TankCommand{
		FORWARD("F"), BACKWARD("B"), ROTATE_LEFT("L"), ROTATE_RIGHT("R"), STOP_MOVING("D"), STOP_ROTATING("T"), IDLE("");

		public boolean processed = false;
		public String sendString = "";
		TankCommand (String sendString){
			this.sendString = sendString;
			processed = false;
		}

	}


}
