package filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import main.Peer;

public class InfoChunk {
	Peer peer;
	
	public InfoChunk(Peer peer) {
		this.peer = peer;
	}

	// Method to load the non volatile memory about the chunk files
	@SuppressWarnings("unchecked")
	public synchronized boolean loadChunksInfo() {
		File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getServerID() + "/" + Peer.CHUNKS_INFO);
		if (file.exists()) {
			try {
				ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
						Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getServerID() + "/" + Peer.CHUNKS_INFO));

				this.peer.setActualReplicationDegrees((ConcurrentHashMap<String, Integer>) serverStream.readObject());
				this.peer.setDesiredReplicationDegrees((ConcurrentHashMap<String, Integer>) serverStream.readObject());
				this.peer.setChunksHosts((ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>) serverStream.readObject());
				this.peer.setChunksStoredSize((ConcurrentHashMap<String, Integer>) serverStream.readObject());

				serverStream.close();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error loading the chunks info file.");
				return false;
			}

			return true;
		} else {
			return false;
		}

	}
	
	// Method to save all the runtime data of the server
	public synchronized void saveChunksInfoFile() {
		try {
			ObjectOutputStream serverStream = new ObjectOutputStream(new FileOutputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getServerID() + "/" + Peer.CHUNKS_INFO));

			serverStream.writeObject(this.peer.getActualReplicationDegrees());
			serverStream.writeObject(this.peer.getDesiredReplicationDegrees());
			serverStream.writeObject(this.peer.getChunksHosts());
			serverStream.writeObject(this.peer.getChunksStoredSize());

			serverStream.close();
		} catch (IOException e) {
			System.err.println("Error writing the server info file.");
		}
	}

	public void storeChunkInfo(int senderID, String fileID, int chunkNr) {
		String hashmapKey = chunkNr + "_" + fileID;

		CopyOnWriteArrayList<Integer> chunkHosts = this.peer.getChunksHosts().get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts == null) {
			chunkHosts = new CopyOnWriteArrayList<Integer>();
			chunkHosts.add(senderID);

			this.peer.getChunksHosts().put(hashmapKey, chunkHosts);
			this.peer.getActualReplicationDegrees().put(hashmapKey, chunkHosts.size());
		} else {
			// Check if senderID is already in the list
			if (!chunkHosts.contains(senderID)) {
				chunkHosts.add(senderID);
				this.peer.getChunksHosts().replace(hashmapKey, chunkHosts);
				this.peer.getActualReplicationDegrees().replace(hashmapKey, chunkHosts.size());
			}
		}
	}
	
	public void removeChunkInfo(String hashmapKey, int senderID) {
		CopyOnWriteArrayList<Integer> chunkHosts = this.peer.getChunksHosts().get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts != null && chunkHosts.contains(senderID)) {
			int index = chunkHosts.indexOf(senderID);
			chunkHosts.remove(index);
			this.peer.getChunksHosts().replace(hashmapKey, chunkHosts);
			this.peer.getActualReplicationDegrees().replace(hashmapKey, chunkHosts.size());
		}
	}

}
