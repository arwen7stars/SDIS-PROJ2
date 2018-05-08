package filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import main.Peer;

public class InfoFile {
	Peer peer;
	
	public InfoFile(Peer peer) {
		this.peer = peer;
	}
	
	// Method to load the non volatile memory about the backup files
	@SuppressWarnings("unchecked")
	public synchronized boolean loadFilesInfo() {
		File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.FILES_INFO);
		if (file.exists()) {
			try {
				ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
						Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.FILES_INFO));
				
				this.peer.setFilesIdentifiers((ConcurrentHashMap<String, String>) serverStream.readObject());
				this.peer.setBackupState((ConcurrentHashMap<String, Boolean>) serverStream.readObject());
				this.peer.setDiskMaxSpace((long) serverStream.readObject());
				this.peer.setDiskUsed((long) serverStream.readObject());

				serverStream.close();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error loading the files info file.");
				return false;
			}

			return true;
		} else {
			return false;
		}
	}
	
	// Method to save all the runtime data of the server
	public synchronized void saveFilesInfoFile() {
		try {
			ObjectOutputStream serverStream = new ObjectOutputStream(new FileOutputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_INFO));

			serverStream.writeObject(this.peer.getFilesIdentifiers());
			serverStream.writeObject(this.peer.getBackupState());
			serverStream.writeObject(this.peer.getDiskSpace());
			serverStream.writeObject(this.peer.getDiskUsed());

			serverStream.close();
		} catch (IOException e) {
			System.err.println("Error writing the server info file.");
		}
	}

	public void removeFileInfo(String fileID) {
		Iterator<String> it = this.peer.getChunkHosts().keySet().iterator();

		while (it.hasNext()) {
			String key = it.next();

			if (key.endsWith(fileID)) {
				it.remove();
			}
		}

		Iterator<String> it2 = this.peer.getActualReplicationDegrees().keySet().iterator();

		while (it2.hasNext()) {
			String key = it2.next();

			if (key.endsWith(fileID)) {
				it2.remove();
			}
		}
	}
}
