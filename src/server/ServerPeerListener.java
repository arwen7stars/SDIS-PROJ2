package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

public class ServerPeerListener implements Runnable {
	private SSLSocket socket;
	
	public ServerPeerListener(SSLSocket s) {
		this.socket = s;
	}

	@Override
	public void run() {
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
			String line = null;
			
	        while((line = in.readLine()) != null){
	            System.out.println(line);
	            out.println(line);
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*while(true) {
			String msg = null;
			
			try {
				msg = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(msg != null) {
				System.out.println(msg);
			}
		}*/
	}
}
