package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import main.Peer;

public class Server {
	private SSLServerSocket socket;
	private static ArrayList<ServerPeerListener> peers;
	
	public static void main(String args[]) {
		peers = new ArrayList<ServerPeerListener>();
		
		Server server = new Server(5000);
	}
	
	public Server(int port) {
		Peer.makeDirectory(Peer.MASTER_FOLDER);
		
		// Set server key and truststore
		/*System.setProperty("javax.net.ssl.trustStore", "SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "SSL/server.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");*/
		
		System.setProperty("javax.net.ssl.keyStore", "SSL/mykeystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "1234567890");
		
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault(); 
		
		try {
			socket = (SSLServerSocket) ssf.createServerSocket(port);
		} catch (IOException e) {
			System.out.println("Problem creating SSL Server Socket");
			System.exit(-1);	// Shutdown the server, because he needs the socket
		}
		
		// MasterServer require client authentication
		socket.setNeedClientAuth(true);	

		new Thread(new ServerChannel(socket)).start();
		
		System.out.println("Server Socket running!");
	}
	
	public static void addPeerListener(SSLSocket s) {
		ServerPeerListener peer_channel = new ServerPeerListener(s);
		new Thread(peer_channel).start();
		
		peers.add(peer_channel);
	}
	
	public static String getPeers() {
		String s = "";
		
		/*for(ServerPeerListener peerChannel : peers)
		{
			if(peer.getPeerID() != null)
			{
				Socket socket = peer.getSocket();
		
				s += socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " ";
				s += peer.getPeerID() + " ";
				s += peer.getMCPort() + " ";
				s += peer.getMDBPort() + " ";
				s += peer.getMDBPort() + " ";
				s += peer.getSenderPort();
				s += "\n";
			}
		}*/
		return s;
	}
}
