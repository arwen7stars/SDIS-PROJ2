package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import peer.Peer;

public class Server {
	private SSLServerSocket socket;
	private static ArrayList<ServerPeerListener> peers;
	
	public static void main(String args[]) {
		peers = new ArrayList<ServerPeerListener>();
		new Server(5000);
	}
	
	public Server(int port) {
		Peer.makeDirectory(Peer.MASTER_FOLDER);
		
		// Set server key and truststore
		//System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore"); UBUNTU
		System.setProperty("javax.net.ssl.trustStore", "SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		//System.setProperty("javax.net.ssl.keyStore", "../SSL/server.keys"); UBUNTU
		System.setProperty("javax.net.ssl.keyStore", "SSL/server.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");		
		
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
	
	public static void removePeerListener(ServerPeerListener spl) {
		peers.remove(spl);
		System.out.println("Server removed dead socket");
	}
	
	public static String getPeers() {
				
		String s = "";
	
		for(ServerPeerListener peer : peers)
		{
			SSLSocket socket = peer.getSocket();
			
			if( !socket.isConnected() || socket.isClosed() || socket.isOutputShutdown() || socket.isInputShutdown() )
				Server.removePeerListener(peer);

			else{
				s += "PEER ";
				s += socket.getInetAddress().getHostAddress() + " ";
				s += peer.getServerID() + " ";
				s += peer.getMCPort() + " ";
				s += peer.getMDBPort() + " ";
				s += peer.getMDBPort() + " ";
				s += "\n";
			}
		}
		s += "DONE";
		return s;
	}
}
