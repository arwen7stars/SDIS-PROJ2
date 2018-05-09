package server;

import java.io.IOException;

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
			
		        System.out.println("Server socket accepted\n");
		        Server.addPeerListener(s);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
