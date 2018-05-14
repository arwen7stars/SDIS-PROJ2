package peer;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import filemanager.InfoChunk;
import filemanager.InfoFile;
import protocols.Backup;
import protocols.Reclaim;
import protocols.Restore;
import protocols.State;

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
	
	// Global configurations
	public static final String PEERS_FOLDER = "Peers";
	public static final String DISK_FOLDER = "DiskPeer";
	public static final String MASTER_FOLDER = "Master";
	
	public static final String SHARED_FOLDER = "Shared";
	public static final String FILES_FOLDER = "Files";
	public static final String CHUNKS_FOLDER = "Chunks";
	public static final String CHUNKS_INFO = "chunks_info.txt";
	public static final String FILES_INFO = "files_info.txt";
	
	// Network configurations
	private SSLSocket socket;
	private DatagramSocket senderSocket;
	private ClientChannel clientChannel;

	// Peer configurations
	private String protocolVersion;
	private int serverID;

	// Multicast configurations
	private ChannelListener mcChannel;
	private ChannelListener mdbChannel;
	private ChannelListener mdrChannel;

	public static enum channelType {
		MC, MDB, MDR
	};

	// Data structures
	private InfoFile fileInfo;
	private InfoChunk chunkInfo;
	
	/**
	 * Maps all the fileIDs with the respective filename for each file whose backup
	 * it has initiated - <FileName><FileID>
	 */
	private ConcurrentHashMap<String, String> filesIdentifiers;

	/**
	 * Stores the backup state for each file whose backup it has initiated -
	 * <FileID><true>
	 */
	private ConcurrentHashMap<String, Boolean> backupState;

	/**
	 * Stores the size file for each chunk - <ChunkNr_FileID><FileSize>
	 */
	private ConcurrentHashMap<String, Integer> chunksStoredSize;

	/**
	 * Stores the actual replication degree of each chunk file -
	 * <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> actualReplicationDegrees;

	/**
	 * Stores the replication degree of each chunk file -
	 * <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> desiredReplicationDegrees;

	/**
	 * Stores who has stored the chunk - <ChunkNr_FileID><List of Peer IDs>
	 */
	private ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> chunksHosts;

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

	/**
	 * Disk space to store chunks
	 */
	private long diskMaxSpace;

	/**
	 * Disk space used to store chunks
	 */
	private long diskUsed;
	
	public Peer(String protocol, int id) throws IOException {
		this.protocolVersion = protocol;
		this.serverID = id;

		// Make peer disk
		String peerDisk = PEERS_FOLDER + "/" + DISK_FOLDER + id;
		String backupFiles = peerDisk + "/" + FILES_FOLDER;
		String chunksFiles = peerDisk + "/" + CHUNKS_FOLDER;
		String sharedFolder = PEERS_FOLDER + "/" + SHARED_FOLDER;

		makeDirectory(peerDisk);
		makeDirectory(backupFiles);
		makeDirectory(chunksFiles);
		makeDirectory(sharedFolder);
		
		this.fileInfo = new InfoFile(this);
		this.chunkInfo = new InfoChunk(this);

		if (!fileInfo.loadFilesInfo()) {
			initializeFilesAttributes();
		}
		if (!chunkInfo.loadChunksInfo()) {
			initializeChunksAttributes();
		}

		this.receivedChunkMessages = new CopyOnWriteArrayList<String>();
		this.receivedPutChunkMessages = new CopyOnWriteArrayList<String>();
		this.restoredChunks = new ConcurrentHashMap<String, byte[]>();
		this.waitRestoredChunks = new CopyOnWriteArrayList<String>();
			
		mcChannel = new ChannelListener(this);
		mdbChannel = new ChannelListener(this);
		mdrChannel = new ChannelListener(this);
		
		connectToMasterServer();
		
		// these channels will receive messages from other peers (other than master peer)
		new Thread(mcChannel).start();
		new Thread(mdbChannel).start();
		new Thread(mdrChannel).start();		

		// allows to send messages to other peers (including to master peer)
		this.senderSocket = new DatagramSocket();
		
		String msg = "REGISTER ";

		msg += serverID + " ";
		msg += mcChannel.getPort() + " ";
		msg += mdbChannel.getPort() + " ";
		msg += mdrChannel.getPort();
		
		clientChannel.sendMessage(msg);
	}

	private void connectToMasterServer() {
		// Set client key and truststore
		//System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore"); UBUNTU
		System.setProperty("javax.net.ssl.trustStore", "SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		//System.setProperty("javax.net.ssl.keyStore", "../SSL/client.keys"); UBUNTU
		System.setProperty("javax.net.ssl.keyStore", "SSL/client.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		
		// connects to master peer by its port
		SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		try {
			socket = (SSLSocket) sf.createSocket("localhost", 5000);
		} catch (IOException e) {
			System.out.println("Can't connect to master server");
			System.exit(-1);	// Shutdown the peer
		}
		
		// allows to receive messages from master peer
		this.clientChannel = new ClientChannel(this, socket);
		new Thread(clientChannel).start();
	}

	// Send delete message to MC channel
	public void sendDeleteRequest(String fileName) {
		String fileID = this.filesIdentifiers.get(fileName);

		if (fileID != null) {
			String message = "DELETE " + this.protocolVersion + " " + this.serverID + " " + fileID + " ";
			message = message + EventHandler.CRLF + EventHandler.CRLF;

			try {
				sendReplyToPeers(Peer.channelType.MC, message.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending delete message to multicast.");
			}

			this.backupState.replace(fileID, false);
			this.fileInfo.removeFileInfo(fileID);
			this.chunkInfo.saveChunksInfoFile();
			this.fileInfo.saveFilesInfoFile();
			System.out.println("Delete finished.");
		} else {
			System.out.println("Error deleting the file, because it wasn't backed up by me.");
		}
	}
	
	public static void makeDirectory(String path) {
		File file = new File(path);

		if (file.mkdirs()) {
			System.out.println("Folder " + path + " created.");
		}
	}

	private void initializeFilesAttributes() {
		this.filesIdentifiers = new ConcurrentHashMap<String, String>();
		this.backupState = new ConcurrentHashMap<String, Boolean>();
		this.diskMaxSpace = 10000000; // 10 Mbs
		this.diskUsed = 0;
	}

	private void initializeChunksAttributes() {
		this.actualReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.desiredReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.chunksHosts = new ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>();
		this.chunksStoredSize = new ConcurrentHashMap<String, Integer>();
	}

	public void sendReplyToPeers(channelType type, byte[] packet) throws IOException {
		this.collectedAllPeers = false;
		this.endpoints = new ArrayList<PeerEndpoint>();
		
		System.out.println("Vou pedir os Peers existentes");
		this.clientChannel.sendMessage("GETPEERS");
		
		while(!this.collectedAllPeers) {}
		
		System.out.println("Já os tenho");
		
		for(PeerEndpoint peer : endpoints) {
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

	public SSLSocket getSocket() {
		return socket;
	}

	public DatagramSocket getSenderSocket() {
		return senderSocket;
	}

	public ClientChannel getClientChannel() {
		return clientChannel;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public int getServerID() {
		return serverID;
	}

	public ChannelListener getMcChannel() {
		return mcChannel;
	}

	public ChannelListener getMdbChannel() {
		return mdbChannel;
	}

	public ChannelListener getMdrChannel() {
		return mdrChannel;
	}

	public InfoFile getFileInfo() {
		return fileInfo;
	}

	public InfoChunk getChunkInfo() {
		return chunkInfo;
	}

	public ConcurrentHashMap<String, String> getFilesIdentifiers() {
		return filesIdentifiers;
	}

	public void setFilesIdentifiers(ConcurrentHashMap<String, String> filesIdentifiers) {
		this.filesIdentifiers = filesIdentifiers;
	}

	public ConcurrentHashMap<String, Boolean> getBackupState() {
		return backupState;
	}

	public void setBackupState(ConcurrentHashMap<String, Boolean> backupState) {
		this.backupState = backupState;
	}

	public ConcurrentHashMap<String, Integer> getChunksStoredSize() {
		return chunksStoredSize;
	}

	public void setChunksStoredSize(ConcurrentHashMap<String, Integer> chunksStoredSize) {
		this.chunksStoredSize = chunksStoredSize;
	}

	public ConcurrentHashMap<String, Integer> getActualReplicationDegrees() {
		return actualReplicationDegrees;
	}

	public void setActualReplicationDegrees(ConcurrentHashMap<String, Integer> actualReplicationDegrees) {
		this.actualReplicationDegrees = actualReplicationDegrees;
	}

	public ConcurrentHashMap<String, Integer> getDesiredReplicationDegrees() {
		return desiredReplicationDegrees;
	}

	public void setDesiredReplicationDegrees(ConcurrentHashMap<String, Integer> desiredReplicationDegrees) {
		this.desiredReplicationDegrees = desiredReplicationDegrees;
	}

	public ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> getChunksHosts() {
		return chunksHosts;
	}

	public void setChunksHosts(ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> chunksHosts) {
		this.chunksHosts = chunksHosts;
	}

	public ConcurrentHashMap<String, byte[]> getRestoredChunks() {
		return restoredChunks;
	}

	public void setRestoredChunks(ConcurrentHashMap<String, byte[]> restoredChunks) {
		this.restoredChunks = restoredChunks;
	}

	public CopyOnWriteArrayList<String> getWaitRestoredChunks() {
		return waitRestoredChunks;
	}

	public void setWaitRestoredChunks(CopyOnWriteArrayList<String> waitRestoredChunks) {
		this.waitRestoredChunks = waitRestoredChunks;
	}

	public CopyOnWriteArrayList<String> getReceivedChunkMessages() {
		return receivedChunkMessages;
	}

	public void setReceivedChunkMessages(CopyOnWriteArrayList<String> receivedChunkMessages) {
		this.receivedChunkMessages = receivedChunkMessages;
	}

	public CopyOnWriteArrayList<String> getReceivedPutChunkMessages() {
		return receivedPutChunkMessages;
	}

	public void setReceivedPutChunkMessages(CopyOnWriteArrayList<String> receivedPutChunkMessages) {
		this.receivedPutChunkMessages = receivedPutChunkMessages;
	}

	public long getDiskMaxSpace() {
		return diskMaxSpace;
	}

	public void setDiskMaxSpace(long diskMaxSpace) {
		this.diskMaxSpace = diskMaxSpace;
	}

	public long getDiskUsed() {
		return diskUsed;
	}

	public void setDiskUsed(long diskUsed) {
		this.diskUsed = diskUsed;
	}

	@Override
	public void backup(String filename, int replicationDegree) throws RemoteException {
		System.out.println("[SERVER " + this.serverID + "] Starting backup protocol...");
		try {
			new Thread(new Backup(filename, replicationDegree, this)).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {
		System.out.println("[SERVER " + this.serverID + "] Starting delete protocol...");
		sendDeleteRequest(filename);
	}

	@Override
	public void restore(String filename) throws RemoteException {
		System.out.println("[SERVER " + this.serverID + "] Starting restore protocol...");
		new Thread(new Restore(filename, this)).start();
	}

	@Override
	public String state() throws RemoteException {
		System.out.println("[SERVER " + this.serverID + "] Starting state feature...");
		System.out.println("State returned.");
		return this.getPeerState();
	}

	@Override
	public void reclaim(int kbytes) throws RemoteException {
		System.out.println("[SERVER " + this.serverID + "] Starting reclaim protocol...");
		System.out.println("Disk used: " + this.diskUsed);
		new Thread(new Reclaim(kbytes, this)).start();
	}

}
