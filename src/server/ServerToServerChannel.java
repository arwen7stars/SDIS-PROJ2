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
	private DataOutputStream out;
	private DataInputStream in;
	private ArrayList<String> peersFromOtherServers;

	public ServerToServerChannel(Socket socket) {
		this.socket = socket;
		this.out = null;
		this.in = null;
		this.peersFromOtherServers = new ArrayList<String>();
	}

	@Override
	public void run() {		
		try {
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// Server disconnected so the socket has to be removed
			Server.removeOtherServer(this);
		}
		
		boolean alive = true;
		
		while(alive) {
			String msg = null;
				
			try {
				msg = in.readUTF();
			} catch (IOException e) {
				// Server disconnected so the socket has to be removed
				Server.removeOtherServer(this);
				alive = false;
			}
				
			if(msg != null) {
				try {
					handleMessage(msg);
				} catch (IOException e) {
					System.out.println("Problem with message from other server.");
				}
			}
		}
	}

	private void handleMessage(String msg) throws IOException {
		String[] msgSplit = msg.split(" ");
		
		switch(msgSplit[0]) {
			case "GETPEERS":	
				out.writeUTF(Server.getPeers());
			
				break;
				
			case "PEER":	
				this.peersFromOtherServers.add(msg);
				
				break;
				
			case "DONE":
				
				break;
				
			case "SAVE_METADATA":
				try
				{
					int peerID = Integer.parseInt(msgSplit[1]);
					
					byte [] bytes  = new byte [256000];
				    int length = in.readInt();	// Number of bytes to read
				    in.read(bytes, 0, length);	// Read bytes to array
				    
				    Server.makePeerDirectory(peerID);
				    File mFile = new File(Server.SERVER_FOLDER + Server.getServerID() + "/" + Server.PEER_FOLDER + peerID + "/" + Server.METADATA_FILE);
				    
				    if(mFile.exists())
				    	mFile.delete();

				    FileOutputStream fout = new FileOutputStream(mFile);
				    fout.write(bytes, 0, length);
				    fout.close();			    
				    
				    System.out.println("Metadata from Peer" + peerID + " stored with " + length + " bytes, received from other server");
				}
				catch(Exception e)
				{
					System.out.println("Problem storing metadata from Peer");
				}
				
				break;
				
			default:
				System.out.println("Server:: Error processing message from other server.");
				break;
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
		try {
			out.writeUTF(message);
		} catch (IOException e) {
			System.out.println("Problem sending message to other server");
		}
	}
	
	public void sendBytes(byte[] message, int numBytes)
	{	
		try
		{
			out.writeInt(numBytes);
			out.write(message, 0, numBytes);
		}
		catch (IOException e)
		{
			System.out.println("Problem sending bytes to other server");
		}
	}

}
