package filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import peer.Peer;

public class MetadataManager {
	
	private Peer peer;
	
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
	 * Disk space to store chunks
	 */
	private long diskMaxSpace;

	/**
	 * Disk space used to store chunks
	 */
	private long diskUsed;
	
	public MetadataManager(Peer peer) {
		this.peer = peer;
		loadMetadata();
	}
	
	// Method to load the non volatile memory about the backup files
	@SuppressWarnings("unchecked")
	public synchronized void loadMetadata() {
		File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getServerID() + "/" + Peer.METADATA_FILE);
		
		if (file.exists()) {
			try {
				ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
						Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getServerID() + "/" + Peer.METADATA_FILE));
				
				//Files Info
				this.filesIdentifiers = (ConcurrentHashMap<String, String>) serverStream.readObject();
				this.backupState = (ConcurrentHashMap<String, Boolean>) serverStream.readObject();
				this.diskMaxSpace = (long) serverStream.readObject();
				this.diskUsed = (long) serverStream.readObject();
				
				//Chunks Info
				this.actualReplicationDegrees = (ConcurrentHashMap<String, Integer>) serverStream.readObject();
				this.desiredReplicationDegrees = (ConcurrentHashMap<String, Integer>) serverStream.readObject();
				this.chunksHosts = (ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>) serverStream.readObject();
				this.chunksStoredSize = (ConcurrentHashMap<String, Integer>) serverStream.readObject();

				serverStream.close();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error loading the metadata file on Peer.");				
			}
		} else {
			initializeAttributes();
		}
	}
	
	private void initializeAttributes() {
		this.filesIdentifiers = new ConcurrentHashMap<String, String>();
		this.backupState = new ConcurrentHashMap<String, Boolean>();
		this.diskMaxSpace = 10000000; // 10 Mbs
		this.diskUsed = 0;
		this.actualReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.desiredReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.chunksHosts = new ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>();
		this.chunksStoredSize = new ConcurrentHashMap<String, Integer>();
	}

	// Method to save all the runtime data of the server
	public synchronized void saveMetadata() {
		try {
			ObjectOutputStream serverStream = new ObjectOutputStream(new FileOutputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getServerID() + "/" + Peer.METADATA_FILE));
			
			//Files Info
			serverStream.writeObject(this.filesIdentifiers);
			serverStream.writeObject(this.backupState);
			serverStream.writeObject(this.diskMaxSpace);
			serverStream.writeObject(this.diskUsed);
			
			//Chunks Info
			serverStream.writeObject(this.actualReplicationDegrees);
			serverStream.writeObject(this.desiredReplicationDegrees);
			serverStream.writeObject(this.chunksHosts);
			serverStream.writeObject(this.chunksStoredSize);
			
			serverStream.close();
		} catch (IOException e) {
			System.err.println("Error writing the server info file.");
		}
	}

	public void removeFileInfo(String fileID) {
		Iterator<String> it = this.chunksHosts.keySet().iterator();

		while (it.hasNext()) {
			String key = it.next();

			if (key.endsWith(fileID)) {
				it.remove();
			}
		}

		Iterator<String> it2 = this.actualReplicationDegrees.keySet().iterator();

		while (it2.hasNext()) {
			String key = it2.next();

			if (key.endsWith(fileID)) {
				it2.remove();
			}
		}
	}
	
	public void storeChunkInfo(int senderID, String fileID, int chunkNr) {
		String hashmapKey = chunkNr + "_" + fileID;

		CopyOnWriteArrayList<Integer> chunkHosts = this.chunksHosts.get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts == null) {
			chunkHosts = new CopyOnWriteArrayList<Integer>();
			chunkHosts.add(senderID);

			this.chunksHosts.put(hashmapKey, chunkHosts);
			this.actualReplicationDegrees.put(hashmapKey, chunkHosts.size());
		} else {
			// Check if senderID is already in the list
			if (!chunkHosts.contains(senderID)) {
				chunkHosts.add(senderID);
				this.chunksHosts.replace(hashmapKey, chunkHosts);
				this.actualReplicationDegrees.replace(hashmapKey, chunkHosts.size());
			}
		}
	}
	
	public void removeChunkInfo(String hashmapKey, int senderID) {
		CopyOnWriteArrayList<Integer> chunkHosts = this.chunksHosts.get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts != null && chunkHosts.contains(senderID)) {
			int index = chunkHosts.indexOf(senderID);
			chunkHosts.remove(index);
			this.chunksHosts.replace(hashmapKey, chunkHosts);
			this.actualReplicationDegrees.replace(hashmapKey, chunkHosts.size());
		}
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


}
