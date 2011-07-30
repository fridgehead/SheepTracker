import java.io.*;
import java.net.*;
import java.util.ArrayList;

class TankServer implements Runnable{

	String clientSentence;
	String capitalizedSentence;
	ServerSocket serverSocket;
	SheepTest parent;

	ArrayList<TankServerThread> threads = new ArrayList<TankServerThread>();


	public TankServer(SheepTest parent){
		this.parent = parent;
		try {
			serverSocket = new ServerSocket(4444);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.out.println("Server thread started");
		}
	}

	public void receivedString(String in){
		//pass a tank update to the parent class
		//System.out.println("Received: " + clientSentence);
		
		TankUpdate t = new TankUpdate();

		if(in.equals("Connected") == false) {
			String[] elements = in.split(",");
			if(elements.length > 2) {
				if(elements[1].equals("C")) {

					try {
						t.type = TankUpdate.COMPASS;
						t.heading = Integer.parseInt(elements[2]) - 180;
						t.tankId = Integer.parseInt(elements[0]);
						parent.tankUpdate(t);
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}


	public void run(){

		while(true)
		{
			Socket connectionSocket;
			try {
				connectionSocket = serverSocket.accept();
				TankServerThread t = new TankServerThread(this,connectionSocket);
				threads.add(t);

				t.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}


	public class TankUpdate{
		public int heading;
		public float lon;
		public float lat;
		public int accuracy;
		public int type;
		public int tankId;

		public static final int COMPASS = 0;
		public static final int POSITION = 1;
		public static final int QUIT = 2;

		public TankUpdate(){}
		
		public String toString(){
			return "tId: " + tankId + " type: " + type;
		}

	}

}