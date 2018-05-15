package server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

import peer.Peer;

public class ServerPeerListener implements Runnable {
	private SSLSocket socket;
	private PrintWriter out;
	private BufferedReader in;
	private int serverID;
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
				System.out.println("\nWaiting for peers to comunicate with me...");
				msg = in.readLine();
				System.out.println("\nReceived message from peer: " + msg);
			} catch (IOException e) {
				// Peer disconnected so the socket has to be removed
				Server.removePeerListener(this);
				alive = false;
			}
				
			if(msg != null) {
				handleMessage(msg.split(" "));
			}
		}
	}
	
	private void handleMessage(String [] msg) {
		switch(msg[0]) {
			case "REGISTER":
				this.serverID = Integer.parseInt(msg[1]);
				this.MCPort = Integer.parseInt(msg[2]);
				this.MDBPort = Integer.parseInt(msg[3]);
				this.MDRPort = Integer.parseInt(msg[4]);
				
				System.out.println("Peer "+serverID+ " registered with success.");
											
				break;
				
			case "GETPEERS":
				out.println(Server.getPeers());
				
			case "GET_METADATA":
				/* TODO
				File file = new File(PATH -> falta o peer ID);
				
				if(file.exists())
				{
					out.println("METADATA");
				}
				else
				{
					out.println("NoMetadata");
				}*/
				
			case "STORE_METADATA":
				//TODO 
				
				break;
				
			default:
				System.out.println("Server:: Error processing message from Peer.");
				break;
		}
	}

	public SSLSocket getSocket() {
		return socket;
	}

	public int getServerID() {
		return serverID;
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
