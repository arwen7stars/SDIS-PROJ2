package server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

public class ServerPeerListener implements Runnable {
	private SSLSocket socket;
	private PrintWriter out;
	private BufferedReader in;
	private int peerID;
	private int MCPort;
	private int MDBPort;
	private int MDRPort;
	
	
	public ServerPeerListener(SSLSocket s) {
		this.socket = s;
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// Peer disconnected so the socket has to be removed
			Server.removePeerListener(this);
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
				// Peer disconnected so the socket has to be removed
				Server.removePeerListener(this);
				alive = false;
			}
				
			if(msg != null) {
				System.out.println("\nReceived message from peer: " + msg);
				handleMessage(msg.split(" "));
			} else {
				System.out.println("Deleted peer " + peerID + " from server");
				Server.removePeerListener(this);
				alive = false;
			}
		}
	}
	
	private void handleMessage(String [] msg) {
		switch(msg[0]) {
			case "REGISTER":
				this.peerID = Integer.parseInt(msg[1]);
				this.MCPort = Integer.parseInt(msg[2]);
				this.MDBPort = Integer.parseInt(msg[3]);
				this.MDRPort = Integer.parseInt(msg[4]);
				
				System.out.println("Peer "+peerID+ " registered with success.");
											
				break;
				
			case "GETPEERS":
				out.println(Server.getPeers());
				
			case "GET_METADATA":
				File file = new File(Server.MASTER_FOLDER + "/" + Server.PEER_FOLDER + this.peerID + "/" + Server.METADATA_FILE);
				
				if(file.exists())
				{
					out.println("METADATA");
			        try
			        {
			        	// Send metadata file to Peer
			        	byte [] bytes  = new byte [(int)file.length()];
			        	FileInputStream fis = new FileInputStream(file);
			        	BufferedInputStream bis = new BufferedInputStream(fis);
			        	bis.read(bytes, 0, bytes.length);
			        	socket.getOutputStream().write(bytes, 0, bytes.length);
			        	
			        	bis.close();
			        	fis.close();
			        }
			        catch(Exception e)
			        {
			        	System.out.println("Problem sending the metadata to the Peer");
			        	break;
			        }
				}
				else
				{
					out.println("METADATA_EMPTY");
				}
				
				break;
				
			case "SAVE_METADATA":
				try
				{
					byte [] array  = new byte [256000];
				    InputStream inputS = socket.getInputStream();
				    int bytesToRead = inputS.read(array);

				    File mFile = new File(Server.MASTER_FOLDER + "/" + Server.PEER_FOLDER + peerID + "/" + Server.METADATA_FILE);

				    if(mFile.exists())
				    	mFile.delete();

				    FileOutputStream fout = new FileOutputStream(mFile);
				    fout.write(array, 0, bytesToRead);
				    fout.close();
				    
				    System.out.println("Metadata from Peer"+peerID+" stored with " + bytesToRead + " bytes");
				}
				catch(Exception e)
				{
					System.out.println("Problem storing metadata from Peer"+peerID);
					break;
				}
				
				break;
				
			default:
				System.out.println("Server:: Error processing message from Peer.");
				break;
		}
	}

	public SSLSocket getSocket() {
		return socket;
	}

	public int getPeerID() {
		return peerID;
	}

	public int getMCPort() {
		return MCPort;
	}

	public int getMDBPort() {
		return MDBPort;
	}

	public int getMDRPort() {
		return MDRPort;
	}
}
