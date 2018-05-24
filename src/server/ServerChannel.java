package server;

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class ServerChannel implements Runnable {
	private SSLServerSocket socket;
	
	public ServerChannel(SSLServerSocket socket) {
		this.socket = socket;
		System.out.println("Server Socket running!");
	}

	@Override
	public void run() {
		while(true) {
			try {
				SSLSocket s = (SSLSocket) socket.accept();
			
		        System.out.println("Peer authenticated with success.\n");
		        Server.addPeerListener(s);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
