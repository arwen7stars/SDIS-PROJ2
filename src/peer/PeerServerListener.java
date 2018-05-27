package peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

public class PeerServerListener implements Runnable {
	private Peer peer;
	private SSLSocket socket;
	PrintWriter out;
	BufferedReader in;
	
	public PeerServerListener(Peer peer, SSLSocket socket) {
		this.peer = peer;
		this.socket = socket;
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {				
		boolean alive = true;

		while(alive) {						
			String msg = null;
			
			try {
				msg = in.readLine();
			} catch (IOException e) {
				System.out.println("Lost connection to Server");
				alive = false;
			}
			
			if(msg != null) {
				handleMessage(msg.split(" "));
			} else alive = false;
		}  
		
		// Tries to reconnect after connection server has been lost
		peer.connectToServer();
	}
	
	private void handleMessage(String[] msg) {
		switch(msg[0]) {
			case "PEER":
				int id = Integer.parseInt(msg[2]);
				
				if(id == this.peer.getID())
					break;
					
				String host = msg[1];
				int portMC = Integer.parseInt(msg[3]);
				int portMDB = Integer.parseInt(msg[4]);
				int portMDR = Integer.parseInt(msg[5]);
				
				this.peer.addPeerEndpoint(host, id, portMC, portMDB, portMDR);
				
				break;
				
			case "DONE":
				this.peer.setCollectedAllPeers(true);
				break;
				
			case "METADATA":
				this.peer.setMetadataResponse(1);
				
				try
				{
					byte [] array  = new byte [256000];
				    InputStream inputS = socket.getInputStream();
				    int bytesToRead = inputS.read(array);

				    File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.METADATA_FILE);

				    if(file.exists())
				    	file.delete();

				    FileOutputStream fout = new FileOutputStream(file);
				    fout.write(array, 0, bytesToRead);
				    fout.close();
				    
				    System.out.println("Metadata received from server with " + bytesToRead + " bytes");
				}
				catch(Exception e)
				{
					System.out.println("Problem storing metadata from server");
					break;
				}
				
				break;
				
			case "METADATA_EMPTY":
				this.peer.setMetadataResponse(0);
				
				break;
				
			default:
				System.out.println("Peer:: Error processing message from server.");
				System.out.println(msg);
				
				break;
		}
	}
	
	public void sendMessage(String message)
	{		
		out.println(message);
	}
	
	public void sendBytes(byte[] message)
	{
		try
		{
			socket.getOutputStream().write(message, 0, message.length);
		}
		catch (IOException e)
		{
			System.out.println("Problem sending bytes to server");
		}
	}
}
