package protocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import encryption.AES;
import filemanager.FileIdentifier;
import peer.EventHandler;
import peer.Peer;

public class Restore implements Runnable {

	private String fileID;
	private String filePath;
	private String newFilePath;
	private Peer peer;
	private final int CHUNK_MAX_SIZE = 64000;
	private int actualChunk;
	private HashMap<Integer, byte[]> fileChunks;

	public Restore(String filename, Peer peer) {
		this.fileChunks = new HashMap<Integer, byte[]>();
		this.filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_FOLDER + "/" + filename;
		this.peer = peer;
		this.newFilePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.RESTORED_FOLDER + "/" + filename;
	}

	@Override
	public void run() {
		// Check if Peer can restore the file
		File file = new File(this.filePath);
		if(file.exists()) {
			this.fileID = new FileIdentifier(this.filePath).toString();
		} else {
			System.out.println("You can't restore a file that you didn't backup.");
			return;
		}		
		
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		boolean restored = false;
		int timeTask = 0;
		this.actualChunk = 0;
		int attempts = 0;

		while (restored == false) {

			// After 3 attempts the restore protocol stops
			if (attempts == 3) {
				System.out.println("File restore finished without success.");
				return;
			}

			// It only sends the getchunk message one time
			if (attempts == 0) {
				byte[] packet = makeGetChunkMessage(this.fileID, this.actualChunk);

				try {
					this.peer.sendReplyToPeers(Peer.channelType.MC, packet);
					this.peer.getWaitRestoredChunks().add(this.actualChunk + "_" + this.fileID);
				} catch (IOException e) {
					System.out.println("Error sending getchunk message");
				}
			}

			Future<Boolean> future = scheduledPool.schedule(isRestored, timeTask, TimeUnit.SECONDS);

			boolean result = false;

			try {
				result = future.get();
			} catch (InterruptedException | ExecutionException e) {
				System.out.println("Error sending getchunk message");
			}

			// If the chunk restored has not yet arrived, the time interval increases 1 second
			if (!result) {
				timeTask = timeTask + 1;
				attempts++;
			} else {
				// Check if it was the last chunk
				byte[] chunk = this.peer.getRestoredChunks().get(this.actualChunk + "_" + this.fileID);
				
				String secretKey = "peer" + this.peer.getID(); 
				AES AES = new AES();
				byte[] chunkDecrypted = AES.decrypt(chunk, secretKey);
				
				this.fileChunks.put(this.actualChunk, chunkDecrypted);

				if (chunk.length < CHUNK_MAX_SIZE) {
					restored = true;
				} else {
					this.actualChunk++;
					timeTask = 0;
					attempts = 0;
				}
			}
		}

		if (restored) {
			restoreFile();
		}
	}
	
	private void restoreFile() {		
		System.out.println("Restore em: "+this.newFilePath);

		try {
			FileOutputStream outputStream = new FileOutputStream(this.newFilePath);
			
			this.fileChunks.forEach( (key, value) -> {
				try {
					outputStream.write(value);
				} catch (IOException e) {
					System.out.println("Error writing the chunk");
				}
			});

			outputStream.close();
		} catch (IOException e) {
			System.out.println("Error saving the restored chunks");
		}
	}

	Callable<Boolean> isRestored = () -> {
		String hashmapKey = this.actualChunk + "_" + this.fileID;
		boolean restoredDone = false;

		if (this.peer.getRestoredChunks().get(hashmapKey) != null) {
			restoredDone = true;
		}

		return restoredDone;
	};

	private byte[] makeGetChunkMessage(String fileID, int chunkNr) {
		String message = "GETCHUNK " + this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID + " "
				+ chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;

		return message.getBytes();
	}

}
