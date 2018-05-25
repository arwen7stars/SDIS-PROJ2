package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ServerToServerChannel implements Runnable {
	
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private ArrayList<String> peersFromOtherServers;

	public ServerToServerChannel(Socket socket) {
		this.socket = socket;
		try {
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// Server disconnected so the socket has to be removed
			Server.removeOtherServer(this);
		}
		this.peersFromOtherServers = new ArrayList<String>();
	}

	@Override
	public void run() {		
		boolean alive = true;
		
		while(alive) {
			String msg = null;
				
			try {
				msg = in.readLine();
			} catch (IOException e) {
				// Peer disconnected so the socket has to be removed
				Server.removeOtherServer(this);
				alive = false;
			}
				
			if(msg != null) {
				handleMessage(msg);
			}
		}
	}

	private void handleMessage(String msg) {
		String[] msgSplit = msg.split(" ");
		switch(msgSplit[0]) {
			case "GETPEERS":	
				out.println(Server.getPeers());
			
				break;
			case "PEER":	
				this.peersFromOtherServers.add(msg);
				
				break;
				
			case "DONE":
				break;
				
			case "SAVE_METADATA":
				try
				{
					//System.out.println("Recebi isto do outro server: "+msg);
					//int peerID = Integer.parseInt(msgSplit[1]);
					
					/*DataInputStream dIn = new DataInputStream(socket.getInputStream());

					int length = dIn.readInt();   
					byte[] message = new byte[length]; // read length of incoming message
					if(length > 0) {					    
					    dIn.readFully(message, 0, message.length); // read the message
					}*/
					
					
					byte [] array  = new byte [256000];
				    InputStream inputS = socket.getInputStream();
				    int bytesToWrite = inputS.read(array);
				    
				    //Server.makePeerDirectory(peerID);
				    File mFile = new File(Server.SERVER_FOLDER + Server.getServerID() + "/" + Server.PEER_FOLDER + 1 + "/" + Server.METADATA_FILE);

				    if(mFile.exists())
				    	mFile.delete();

				    FileOutputStream fout = new FileOutputStream(mFile);
				    fout.write(array, 0, bytesToWrite);
				    fout.close();			    
				    
				    System.out.println("Metadata from Peer stored with " + bytesToWrite + " bytes - RECEIVED FROM OTHER SERVER");
				}
				catch(Exception e)
				{
					System.out.println("Problem storing metadata from Peer");
					break;
				}
				
				break;
				
			default:
				System.out.println("Server:: Error processing message from other server.");
				System.out.println("Mensagem foi: "+ msgSplit[0]);
		}
	}
	
	public ArrayList<String> getPeersFromOtherServers() {
		return this.peersFromOtherServers;
	}
	
	public void cleanPeersFromOtherServers() {
		this.peersFromOtherServers = new ArrayList<String>();
	}
	
	public void sendMessage(String message)
	{
		out.println(message);
	}
	
	public void sendBytes(byte[] message, int numBytes)
	{
		/*DataOutputStream dOut;
		try {
			dOut = new DataOutputStream(socket.getOutputStream());
			
			dOut.writeInt(numBytes); // write length of the message
			dOut.write(message); 
			
			System.out.println("Enviei o outro server: "+numBytes + " bytes");
		} catch (IOException e) {
			System.out.println("Problem sending bytes to server");
		}*/

		
		try
		{
			socket.getOutputStream().write(message, 0, numBytes);
			System.out.println("Vou mandar para o outro server: "+numBytes + " bytes");
		}
		catch (IOException e)
		{
			System.out.println("Problem sending bytes to server");
		}
	}

}
