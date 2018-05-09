package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;

public class ClientChannel implements Runnable {
	private Peer peer;
	private SSLSocket socket;
	PrintWriter out;
	
	public ClientChannel(Peer peer, SSLSocket socket) {
		this.peer = peer;
		this.socket = socket;
	}
	
	@Override
	public void run() {
		out = null;
		BufferedReader in = null;

		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scanner scanner = new Scanner(System.in);

		while(true) {			
			System.out.print("Enter something: ");
			String input = scanner.nextLine();
            out.println(input);
            
            System.out.println("Sent msg to server.");
            try {
				System.out.println("Response: " + in.readLine());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			/*String msg = null;
			
			try {
				System.out.println("Peer socket " + peer.getID() + " listening to messages from other sockets!");

				msg = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(msg != null) {
				System.out.println(msg);
			}*/
			
		}            
	}
	
	public void sendMessage(String message)
	{
		out.println(message);
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