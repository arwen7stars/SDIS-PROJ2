package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerToServerChannel implements Runnable {
	
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	public ServerToServerChannel(Socket socket) {
		this.socket = socket;
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// Server disconnected so the socket has to be removed
			Server.removeOtherServer(this);
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
				Server.removeOtherServer(this);
				alive = false;
			}
				
			if(msg != null) {
				handleMessage(msg.split(" "));
			}
		}
	}

	private void handleMessage(String[] split) {
		// TODO Auto-generated method stub
	}

}
