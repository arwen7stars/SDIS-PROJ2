package peer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;

import server.Server;

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
		//Scanner scanner = new Scanner(System.in);
		
		boolean alive = true;

		while(alive) {			
			/*System.out.print("Enter something: ");
			String input = scanner.nextLine();
            out.println(input);
            
            System.out.println("Sent msg to server.");
            try {
				System.out.println("Response: " + in.readLine());
			} catch (IOException e1) {
				e1.printStackTrace();
			}*/
			
			String msg = null;
			
			try {
				System.out.println("Peer socket " + peer.getServerID() + " listening to messages from the server...");

				msg = in.readLine();
			} catch (IOException e) {
				System.out.println("Lost connection to Server");
				alive = false;
			}
			
			if(msg != null) {
				String[] parts = msg.split(" ");
				
				switch(parts[0]) {
					case "PEER":
						int id = Integer.parseInt(parts[2]);
						
						if(id == this.peer.getServerID())
							break;
							
						String host = parts[1];
						int portMC = Integer.parseInt(parts[3]);
						int portMDB = Integer.parseInt(parts[4]);
						int portMDR = Integer.parseInt(parts[5]);
						
						this.peer.addPeerEndpoint(host, id, portMC, portMDB, portMDR);
						break;
					case "DONE":
						this.peer.setCollectedAllPeers(true);
						break;
					default:
						System.out.println("Client:: Error processing message.");
				}
			}
			
		}  
		
		peer.connectToMasterServer();
	}
	
	public void sendMessage(String message)
	{		
		out.println(message);
		System.out.println("Sent message " + message);
	}
	
	public void sendBytes(byte[] message)
	{
		DataOutputStream dOut = null;
		try {
			dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeInt(message.length); 							// write length of the message
			dOut.write(message);           							// write the message
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}