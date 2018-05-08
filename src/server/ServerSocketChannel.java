package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;

public class ServerSocketChannel implements Runnable {
	private SSLServerSocket socket;
	
	public ServerSocketChannel(SSLServerSocket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		while(true) {
			try {
				System.out.println("Waiting for socket...");
				Socket s = socket.accept();
			
		        System.out.println("Server socket " + s.getLocalPort() + " accepted");
		        
		        // Add socket to peer channel array and create a new thread that will listen to messages
		        
		        /*PrintWriter out = new PrintWriter(s.getOutputStream(), true);
		        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		        String line = in.readLine();
	            System.out.println(line);
	            out.println("resp"+line);*/
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
