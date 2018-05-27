package server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Server {
	private SSLServerSocket socket;
	private static ArrayList<ServerPeerListener> peers;
	private static ArrayList<ServerToServerChannel> otherServers;
	
	public static final String SERVER_FOLDER = "Server";
	public static final String METADATA_FILE = "metadata.ser";
	public static final String PEER_FOLDER = "Peer";
	private static int serverID;
	private int serverPort;
	
	public static void main(String args[]) {
		if(args.length != 2){
			System.out.println("Invalid usage, wrong number of args");
			System.exit(-1);
		}
		
		int serverID = Integer.valueOf(args[0]);
		int serverPort = Integer.valueOf(args[1]);
		
		if(serverPort < 3000 || serverPort > 3002){
			System.out.println("Must specify a port between 3000 and 3002");
			System.exit(-1);
		}
		
		peers = new ArrayList<ServerPeerListener>();
		new Server(serverID, serverPort);
	}
	
	public Server(int id, int port) {
		serverID = id;
		this.serverPort = port;
		makeDirectory(Server.SERVER_FOLDER + serverID);
		
		// Set server key and truststore
		System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore"); // UBUNTU
		//System.setProperty("javax.net.ssl.trustStore", "SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "../SSL/server.keys"); // UBUNTU
		//System.setProperty("javax.net.ssl.keyStore", "SSL/server.keys");
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
		
		connectToOtherServers();
	}
	
	private void connectToOtherServers() {
		otherServers = new ArrayList<ServerToServerChannel>();
		
		boolean serverLive = true;
		Socket socket = null;
		int otherServerPort = getServerPort(serverPort, "FIRST");
		
		// Communication between servers is at the port + 1000, to don't conflict with authentication port for peers
		
		try
		{
			socket = new Socket(this.socket.getInetAddress(), otherServerPort + 1000);
		}
		catch(Exception e)
		{
			serverLive = false;
		}
		
		if(socket != null && serverLive) {
			// Start thread to listen the server
			addOtherServer(socket);
			System.out.println("Connected to other server.");
		}
		
		otherServerPort = getServerPort(serverPort, "SECOND");
		socket = null;
		serverLive = true;
		
		try
		{
			socket = new Socket(this.socket.getInetAddress(), otherServerPort + 1000);
		}
		catch(Exception e)
		{
			serverLive = false;
		}
		
		if(socket != null && serverLive) {
			// Start thread to listen the server
			addOtherServer(socket);			
			System.out.println("Connected to other server.");
		}

		// Allow the others servers to connect with me
		try
		{
			ServerSocket serverSocket = new ServerSocket(serverPort + 1000);
			
			ServerToServerListener otherServerListener = new ServerToServerListener(serverSocket);
			new Thread(otherServerListener).start();
		}
		catch (IOException e)
		{
			System.out.println("Problem opening connection to other servers");
			System.exit(-1);
		}
	}
	
	private int getServerPort(int port, String next) {
		int otherPort = -1;
		switch (port) {
	        case 3000:          	
	            if(next.equals("FIRST")) {
	            	otherPort = 3001;
	            } else {
	            	otherPort = 3002;
	            }
	        	break;
	        case 3001:  
	            if(next.equals("FIRST")) {
	            	otherPort = 3000;
	            } else {
	            	otherPort = 3002;
	            }
	            break;
	            
	        case 3002:  
	            if(next.equals("FIRST")) {
	            	otherPort = 3000;
	            } else {
	            	otherPort = 3001;
	            }
	        	break;
	        
	        default: 
	        	break;
		}
		
		return otherPort;
	}

	private void makeDirectory(String path) {
		File file = new File(path);

		if (file.mkdirs()) {
			System.out.println("Folder " + path + " created.");
		}
	}
	
	public static void addPeerListener(SSLSocket s) {
		ServerPeerListener peer_channel = new ServerPeerListener(s);
		new Thread(peer_channel).start();
		
		peers.add(peer_channel);
	}

	public static void removePeerListener(int peerID) {
		for(ServerPeerListener peer : peers) {
			if (peer.getPeerID() == peerID) {
				peers.remove(peer);
				break;
			}
		}
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
			
			if(!socket.isConnected() || socket.isClosed() || socket.isOutputShutdown() || socket.isInputShutdown() )
				Server.removePeerListener(peer);

			else{
				s += "PEER ";
				s += socket.getInetAddress().getHostAddress() + " ";
				s += peer.getPeerID() + " ";
				s += peer.getMCPort() + " ";
				s += peer.getMDBPort() + " ";
				s += peer.getMDRPort() + " ";
				s += "\n";
			}
		}
		s += "DONE";
		return s;
	}
	
	public static ArrayList<String> getPeersFromOtherServers()
	{
		// Peers from other servers, this server don't have the socket
		ArrayList<String> peersFromOthersServers = new ArrayList<String>();
		
		for(ServerToServerChannel serverChannel : otherServers)
		{
			serverChannel.sendMessage("GETPEERS");
			
			// Schedule task to wait response from the other server
			ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
			Future<Boolean> future = scheduledPool.schedule(waitForPeers, 500, TimeUnit.MILLISECONDS);
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {}
			
			ArrayList<String> otherPeers = serverChannel.getPeersFromOtherServers();
			for(String peer : otherPeers)
			{
				peersFromOthersServers.add(peer);
			}
			serverChannel.cleanPeersFromOtherServers();
		}
		
		return peersFromOthersServers;
	}
	
	static Callable<Boolean> waitForPeers = () -> {				
        return true;
	};
	
	public static ArrayList<ServerToServerChannel> getOtherServers() {
		return otherServers;
	}

	public static void removeOtherServer(ServerToServerChannel channel) {
		otherServers.remove(channel);
	}

	public static void addOtherServer(Socket s) {
		ServerToServerChannel otherServerChannel = new ServerToServerChannel(s);
		new Thread(otherServerChannel).start();
		
		otherServers.add(otherServerChannel);
	}
	
	public static void makePeerDirectory(int peerID) {
		File file = new File(Server.SERVER_FOLDER + serverID + "/" + Server.PEER_FOLDER + peerID);
		file.mkdirs();
	}
	
	public static int getServerID() {
		return serverID;
	}
}
