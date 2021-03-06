import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;


public class TankServerThread extends Thread{

	TankServer parent; 
	Socket socket;
	String clientSentence = "";
	BufferedReader inFromClient;
	public boolean running = false;


	public TankServerThread(TankServer parent, Socket s){
		super();
		this.parent = parent;
		socket = s;
		running = true;
	}

	public void end(){

		try{
			socket.close();
			System.out.println("killing tank thread");
			running = false;
			parent.receivedString("QUIT");
		} catch (SocketException e){
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void run(){
		while(running){

			try {
				inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));


				clientSentence = inFromClient.readLine();
				if(clientSentence != null){
					parent.receivedString(clientSentence.trim());
				} else {
					end();
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
