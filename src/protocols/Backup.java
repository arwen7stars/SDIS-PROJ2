package protocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import encryption.AES;
import filemanager.FileChunk;
import filemanager.FileIdentifier;
import peer.*;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Backup implements Runnable {
	private String fileID;
	private String fileName;
	private String filePath;
	private int replicationDegree;
	private Peer peer;

	private final int CHUNK_MAX_SIZE = 64000;
	private final long FILE_MAX_SIZE = 64000000000L;

	public Backup(String file, int replication, Peer peer) throws FileNotFoundException, IOException {
		this.filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_FOLDER + "/" + file;
		this.replicationDegree = replication;
		this.peer = peer;
		this.fileName = file;
		this.fileID = new FileIdentifier(filePath).toString();
	}
	
	@Override
	public void run() {
		//Check if the file has been backed up before
		if(this.peer.getMetadataManager().getFilesIdentifiers().containsValue(this.fileID) && this.peer.getMetadataManager().getBackupState().get(this.fileID) == true) {
			System.out.println("You have already done the backup of this file.");
			return;
		} 
		
		//Check the file size limit
		if(new File(this.filePath).length() >= FILE_MAX_SIZE) {
			System.out.println("File size exceeds the limit.");
		}
		
		String oldFileID = this.peer.getMetadataManager().getFilesIdentifiers().get(this.fileName);
		
		//File was modified, so the system needs to delete the old version
		if(oldFileID != null && !oldFileID.equals(this.fileID)) {
			this.peer.sendDeleteRequest(this.fileName);
		}
		
		try {
			splitFile();
		} catch (IOException e) {
			System.out.println("File for backup not found.");
		}
	}

	private void splitFile() throws FileNotFoundException, IOException {
		byte[] buffer = new byte[CHUNK_MAX_SIZE];
		int chunkNr = 0;
		List<Future<Boolean>> threadResults = new ArrayList<Future<Boolean>>();
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(100);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Vou comecar o backup: " + dateFormat.format(date));		
		
		this.peer.getMetadataManager().getFilesIdentifiers().put(this.fileName, this.fileID);
		this.peer.getMetadataManager().getBackupState().put(this.fileID, false);
		
		File file = new File(this.filePath);
		
		boolean needChunkZero = false;
		
		if((file.length() % CHUNK_MAX_SIZE) == 0) {
			needChunkZero = true;
		}

		try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
			int size = 0;
			
			while ((size = bis.read(buffer)) > 0) {
				byte[] content = new byte[size];
				System.arraycopy(buffer, 0, content, 0, size);	
				
				
				//Chunk encryption 
				String secretKey = "peer" + this.peer.getID();   
				AES AES = new AES();
				byte[] contentEncrypted = AES.encrypt(content, secretKey) ;
				
				
				
				Future<Boolean> result = scheduledPool.submit(new FileChunk(this.fileID, chunkNr, contentEncrypted, this.replicationDegree, this.peer));
				threadResults.add(result);

				this.peer.getMetadataManager().getDesiredReplicationDegrees().put(chunkNr + "_" + this.fileID, this.replicationDegree);
				chunkNr++;
			}
			
			//Add last chunk with zero length
			if(needChunkZero) {
				byte[] empty = new byte[0];
				
				Future<Boolean> result = scheduledPool.submit(new FileChunk(this.fileID, chunkNr, empty, this.replicationDegree, this.peer));
				threadResults.add(result);
				this.peer.getMetadataManager().getDesiredReplicationDegrees().put(chunkNr + "_" + this.fileID, this.replicationDegree);
				chunkNr++;
			}
		}
		
		boolean backupDone = waitBackupResult(scheduledPool, threadResults);
		
		if(backupDone) {
			Date date2 = new Date();
			System.out.println("Backup completed. " + dateFormat.format(date2));
			this.peer.getMetadataManager().getBackupState().replace(fileID, true);
		} else {
			Date date2 = new Date();
			System.out.println("Backup was not completed. " + dateFormat.format(date2));
		}
		
		this.peer.getMetadataManager().saveMetadata();
	}

	//Method that waits for chunk threads to finish
	private boolean waitBackupResult(ScheduledExecutorService scheduledPool, List<Future<Boolean>> threadResults) {
		boolean backupDone = false;
		for (Future<Boolean> result : threadResults) {
			try {
				if(!result.get()) {
					backupDone = false;
					scheduledPool.shutdownNow();
					break;
				} else {
					backupDone = true;
				}
			} catch (InterruptedException | ExecutionException e) {}	
		}
		
		return backupDone;
	}

}
