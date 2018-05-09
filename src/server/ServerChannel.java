package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class ServerChannel implements Runnable {
	private SSLServerSocket socket;
	
	public ServerChannel(SSLServerSocket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		while(true) {
			try {
				System.out.println("Waiting for socket...");
				SSLSocket s = (SSLSocket) socket.accept();
			
		        System.out.println("Server socket " + s.getLocalPort() + " accepted");
		        Server.addPeerListener(s);
		        
		        // Add socket to peer channel array and create a new thread that will listen to messages
		        
		        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
		        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		        String line = in.readLine();
	            System.out.println(line);
	            out.println("resp"+line);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
