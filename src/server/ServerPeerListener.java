package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

public class ServerPeerListener implements Runnable {
	private SSLSocket socket;
	
	private int serverID;
	private int MCPort;
	private int MDBPort;
	private int MDRPort;
	
	public ServerPeerListener(SSLSocket s) {
		this.socket = s;
	}

	@Override
	public void run() {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
			/*String line = null;
			
	        while((line = in.readLine()) != null){
	            System.out.println(line);
	            out.println(line);
	        }*/
			
			while(true) {
				String msg = null;
				
				try {
					System.out.println("\nWaiting for peers to comunicate with me...");
					msg = in.readLine();
					System.out.println("\nReceived message from peer: " + msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if(msg != null) {
					String[] parts = msg.split(" ");
					
					switch(parts[0]) {
						case "REGISTER":
							this.serverID = Integer.parseInt(parts[1]);
							this.MCPort = Integer.parseInt(parts[2]);
							this.MDBPort = Integer.parseInt(parts[3]);
							this.MDRPort = Integer.parseInt(parts[4]);
							
							System.out.println("Peer "+serverID+ " registered with success.");
														
							break;
						case "GETPEERS":
							out.println(Server.getPeers());
						default:
							System.out.println("Server:: Error processing message.");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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
