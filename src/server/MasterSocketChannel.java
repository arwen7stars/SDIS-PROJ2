package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MasterSocketChannel implements Runnable
{
	private Socket socket;
	
	public MasterSocketChannel(Socket socket) 
	{
		this.socket = socket;
	}
	
	@Override
	public void run() 
	{
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(true) {
			String msg = null;
			
			try {	
				msg = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(msg != null) {
				out.println(msg);
			}
			
		}
	}
	
	

}
