package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import filemanager.*;
import protocols.*;

public class Peer implements IRMI {	
	public class PeerEndpoint {
		public String host;
		public int id;
		public int portMC;
		public int portMDB;
		public int portMDR;
	};
	
	private volatile ArrayList<PeerEndpoint> endpoints;
	private volatile boolean collectedAllPeers;
	private volatile int metadataServer; //-1 waiting for answer, 1 exists, 0 don't exist
	
	// Global configurations
	public static final String PEERS_FOLDER = "Peers";
	public static final String DISK_FOLDER = "DiskPeer";
	
	public static final String FILES_FOLDER = "MyFiles";
	public static final String RESTORED_FOLDER = "RestoredFiles";
	public static final String CHUNKS_FOLDER = "Chunks";
	public static final String METADATA_FILE = "metadata.ser";
	
	// Network configurations
	private String hostIP;
	private SSLSocket socket;
	private DatagramSocket senderSocket;
	private PeerServerListener serverChannel;

	// Peer configurations
	private int peerID;
	private String protocolVersion;

	// Multicast configurations
	private PeerChannel mcChannel;
	private PeerChannel mdbChannel;
	private PeerChannel mdrChannel;

	public static enum channelType {
		MC, MDB, MDR
	};

	// Data structures
	private MetadataManager dataManager;
	
	/**
	 * Stores the restored chunks received - <ChunkNr_FileID><File Bytes>
	 */
	private ConcurrentHashMap<String, byte[]> restoredChunks;

	/**
	 * Stores the chunks file that are waiting for - <ChunkNr_FileID>
	 */
	private CopyOnWriteArrayList<String> waitRestoredChunks;

	/**
	 * Stores the messages chunks that has received - <ChunkNr_FileID>
	 */
	private CopyOnWriteArrayList<String> receivedChunkMessages;

	/**
	 * Stores the messages chunks that has received - <ChunkNr_FileID>
	 */
	private CopyOnWriteArrayList<String> receivedPutChunkMessages;
	
	public Peer(String protocol, int id, String hostIP) throws IOException, InterruptedException, ExecutionException {
		this.protocolVersion = protocol;
		this.peerID = id;
		this.hostIP = hostIP;

		// make peer disk
		String peerDisk = PEERS_FOLDER + "/" + DISK_FOLDER + id;
		String backupFiles = peerDisk + "/" + FILES_FOLDER;
		String chunksFiles = peerDisk + "/" + CHUNKS_FOLDER;
		String restoredFile = peerDisk + "/" + RESTORED_FOLDER;

		makeDirectory(peerDisk);
		makeDirectory(backupFiles);
		makeDirectory(chunksFiles);
		makeDirectory(restoredFile);

		this.receivedChunkMessages = new CopyOnWriteArrayList<String>();
		this.receivedPutChunkMessages = new CopyOnWriteArrayList<String>();
		this.restoredChunks = new ConcurrentHashMap<String, byte[]>();
		this.waitRestoredChunks = new CopyOnWriteArrayList<String>();
			
		mcChannel = new PeerChannel(this);
		mdbChannel = new PeerChannel(this);
		mdrChannel = new PeerChannel(this);
		
		connectToServer();
		
		// these channels will receive messages from other peers (other than master peer)
		new Thread(mcChannel).start();
		new Thread(mdbChannel).start();
		new Thread(mdrChannel).start();		

		// allows to send messages to other peers (including the master peer)
		this.senderSocket = new DatagramSocket();
		
		// manage Metadata
		getMetadata();
		new Thread(new BackupMetadata(this)).start();
	}
	
	public void getMetadata() throws InterruptedException, ExecutionException {
		File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peerID + "/" + Peer.METADATA_FILE);
		
		if (file.exists()) {
			try {
				ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
						Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peerID + "/" + Peer.METADATA_FILE));
				dataManager = (MetadataManager) serverStream.readObject();
				
				serverStream.close();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error loading the metadata file on Peer.");				
			}
		} else {
			// Ask metadata to the Server
			metadataServer = -1;
			serverChannel.sendMessage("GET_METADATA");
			
			// Schedule task to check response from server and load metadata
			ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
			scheduledPool.schedule(loadMetadata, 1000, TimeUnit.MILLISECONDS);
		}
	}
	
	Runnable loadMetadata = () -> {
		if(this.metadataServer == 1) {
			try {
				ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
						Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peerID + "/" + Peer.METADATA_FILE));
				dataManager = (MetadataManager) serverStream.readObject();
				
				serverStream.close();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error loading the metadata file on Peer.");				
			}
		} else {
			// Create MetadataManager empty
			dataManager = new MetadataManager(this.peerID);
		}
	};

	public void connectToServer() {
		// Set client key and truststore
		System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore"); // UBUNTU
		//System.setProperty("javax.net.ssl.trustStore", "SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "../SSL/client.keys"); // UBUNTU
		//System.setProperty("javax.net.ssl.keyStore", "SSL/client.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		
		SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		
		// Tries to connect to one server
		Random rand = new Random();
		int  n = rand.nextInt(3); // Number between 0 and 2
		int serverPort = 3000 + n;		
		
		boolean connected = false;
		while(!connected)
		{			
			try
			{
				socket = (SSLSocket) sf.createSocket(this.hostIP, serverPort);
				connected = true;
			}
			catch (IOException e)
			{
				connected = false;
				serverPort++;
				
				// Exceeded the limit
				if(serverPort == 3003) {
					serverPort = 3000;
				}
				
				// Went through all the ports
				if(serverPort == 3000 + n)
				{
					System.out.println("Can't connect to any server.");
					System.exit(-1); // Shutdown the peer
				}
			}
		}
		
		System.out.println("Connected to server with port: "+serverPort);
		
		// allows to receive messages from master peer
		this.serverChannel = new PeerServerListener(this, socket);
		new Thread(serverChannel).start();
		notifyAuthenticationToServer();
	}
	
	private void notifyAuthenticationToServer() {
		String msg = "REGISTER ";

		msg += peerID + " ";
		msg += mcChannel.getPort() + " ";
		msg += mdbChannel.getPort() + " ";
		msg += mdrChannel.getPort();
		
		serverChannel.sendMessage(msg);
	}

	// Send delete message to MC channel
	public void sendDeleteRequest(String fileName) {
		String fileID = this.getMetadataManager().getFilesIdentifiers().get(fileName);

		if (fileID != null) {
			String message = "DELETE " + this.protocolVersion + " " + this.peerID + " " + fileID + " ";
			message = message + EventHandler.CRLF + EventHandler.CRLF;

			try {
				sendReplyToPeers(Peer.channelType.MC, message.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending delete message to multicast.");
			}

			this.getMetadataManager().getBackupState().replace(fileID, false);
			this.getMetadataManager().removeFileInfo(fileID);
			this.getMetadataManager().saveMetadata();
			System.out.println("Delete finished.");
		} else {
			System.out.println("Error deleting the file, because it wasn't backed up by me.");
		}
	}
	
	private void makeDirectory(String path) {
		File file = new File(path);

		if (file.mkdirs()) {
			System.out.println("Folder " + path + " created.");
		}
	}

	public synchronized void sendReplyToPeers(channelType type, byte[] packet) throws IOException {
		this.collectedAllPeers = false;
		this.endpoints = new ArrayList<PeerEndpoint>();
	
		this.serverChannel.sendMessage("GETPEERS");
		
		while(!this.collectedAllPeers) {}
		
		for(PeerEndpoint peer : endpoints) {
			if (peer.id == peerID) {					// Can't send messages to self
				continue;
			}

			InetAddress address = InetAddress.getByName(peer.host);
			
			int port = -1;
			switch (type) {
			case MC:
				port = peer.portMC;
				break;
			case MDB:
				port = peer.portMDB;
				break;
			case MDR:
				port = peer.portMDR;
				break;
			}

			DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, address, port);
			senderSocket.send(sendPacket);
		}
	}
	
	public void addPeerEndpoint(String host, int id, int portMC, int portMDB, int portMDR) {
		PeerEndpoint peer = new PeerEndpoint();
		
		peer.host = host;
		peer.id = id;
		peer.portMC = portMC;
		peer.portMDB = portMDB;
		peer.portMDR = portMDR;
		
		endpoints.add(peer);
	}
	
	public String getPeerState() {
		return new State(this).getState();
	}
	
	public ArrayList<PeerEndpoint> getEndpoints() {
		return endpoints;
	}

	public boolean isCollectedAllPeers() {
		return collectedAllPeers;
	}

	public void setCollectedAllPeers(boolean collectedAllPeers) {
		this.collectedAllPeers = collectedAllPeers;
	}
	
	public void setMetadataResponse(int response) {
		this.metadataServer = response;
	}

	public SSLSocket getSocket() {
		return socket;
	}

	public DatagramSocket getSenderSocket() {
		return senderSocket;
	}

	public PeerServerListener getServerChannel() {
		return serverChannel;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public int getID() {
		return peerID;
	}

	public String getHostIP() {
		return hostIP;
	}

	public PeerChannel getMcChannel() {
		return mcChannel;
	}

	public PeerChannel getMdbChannel() {
		return mdbChannel;
	}

	public PeerChannel getMdrChannel() {
		return mdrChannel;
	}
	
	public MetadataManager getMetadataManager() {
		return dataManager;
	}

	public ConcurrentHashMap<String, byte[]> getRestoredChunks() {
		return restoredChunks;
	}

	public CopyOnWriteArrayList<String> getWaitRestoredChunks() {
		return waitRestoredChunks;
	}

	public CopyOnWriteArrayList<String> getReceivedChunkMessages() {
		return receivedChunkMessages;
	}

	public CopyOnWriteArrayList<String> getReceivedPutChunkMessages() {
		return receivedPutChunkMessages;
	}

	@Override
	public void backup(String filename, int replicationDegree) throws RemoteException {
		System.out.println("[SERVER " + this.peerID + "] Starting backup protocol...");
		try {
			new Thread(new Backup(filename, replicationDegree, this)).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {
		System.out.println("[SERVER " + this.peerID + "] Starting delete protocol...");
		sendDeleteRequest(filename);
	}

	@Override
	public void restore(String filename) throws RemoteException {
		System.out.println("[SERVER " + this.peerID + "] Starting restore protocol...");
		new Thread(new Restore(filename, this)).start();
	}

	@Override
	public String state() throws RemoteException {
		System.out.println("[SERVER " + this.peerID + "] Starting state feature...");
		System.out.println("State returned.");
		return this.getPeerState();
	}

	@Override
	public void reclaim(int kbytes) throws RemoteException {
		System.out.println("[SERVER " + this.peerID + "] Starting reclaim protocol...");
		System.out.println("Disk used: " + this.getMetadataManager().getDiskUsed());
		new Thread(new Reclaim(kbytes, this)).start();
	}

}
