package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerToServerListener implements Runnable {
	
	private ServerSocket serverSocket;
	
	public ServerToServerListener(ServerSocket socket) {
		this.serverSocket = socket;
	}

	@Override
	public void run() {
		while(true) {
			Socket socket = null;
			
			try
			{
				socket = (Socket) serverSocket.accept();
			}
			catch (IOException e)
			{
				System.out.println("Problem waiting for a socket to connect.");
			}
			
			if(socket != null) {
				System.out.println("Other server has connected to me.");
				Server.addOtherServer(socket);
			}
		}
	}

}
