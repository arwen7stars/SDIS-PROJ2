package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import main.Peer;

public class Server {
	private ServerSocket socket;
	private static MasterSocketChannel masterPeer;
	private static ArrayList<Peer> peers;
	
	public static void main(String args[]) {
		if(args.length != 1){
			System.out.println("Wrong number of args.");
			System.exit(1);
		}
		Server server = new Server(5000);
	}
	
	public Server(int port) {
		Peer.makeDirectory(Peer.MASTER_FOLDER);
		
		System.setProperty("javax.net.ssl.keyStore", "SSL/mykeystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "1234567890");
		
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault(); 
		
		try {
			socket = (ServerSocket) ssf.createServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//socket.setNeedClientAuth(true);		// socket needs to be SSLSocket for this to work...

		new Thread(new ServerSocketChannel(socket)).start();
		
		System.out.println("Server Socket running!");

	}
}
