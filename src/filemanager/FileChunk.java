package filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import peer.EventHandler;
import peer.Peer;

public class FileChunk implements Callable<Boolean> {
	
	private String fileID;
	private int number;
	private byte[] content;
	private int replicationDegree;
	private Peer peer;
	
	public FileChunk(String fileID, int chunkNr, byte[] content, int replication, Peer peer) {
		this.fileID = fileID;
		this.number = chunkNr;
		this.content = content;
		this.replicationDegree = replication;
		this.peer = peer;
	}

	@Override
	public Boolean call() {
		byte [] packet = makePutChunkRequest();
		try {
			this.peer.sendReplyToPeers(Peer.channelType.MDB, packet);
		} catch (IOException e1) {
			System.out.println("Error sending putchunk message");
		}
		
		boolean result = false;
		
		try {
			result = checkStoredMessages();
		} catch (InterruptedException | ExecutionException e) {}
		
		return result;
	}
	
	public static byte[] getChunk(int peerID, String fileID, String chunkNr) {
		File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peerID + "/" + Peer.CHUNKS_FOLDER + "/"
				+ chunkNr + "_" + fileID);

		byte[] chunkBytes = new byte[(int) file.length()];

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			fis.read(chunkBytes);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return chunkBytes;
	}


	private boolean checkStoredMessages() throws InterruptedException, ExecutionException {		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		boolean result = false;
		int attempts = 0;
		int timeTask = 1;
		
		while(result == false && attempts < 5) {	
			Future<Boolean> future = executor.schedule(isBackedUp, timeTask, TimeUnit.SECONDS);
			
			result = future.get();
			
			//If the desired replication degree is not fulfilled, the time interval doubles
			if(!result) {
				timeTask = timeTask * 2;
				attempts++;
			} 
		}
		
		executor.shutdownNow();
		return result;
	}
	
	//Task that checks if the chunk has the desired replication degree
	Callable<Boolean> isBackedUp = () -> {
		String hashmapKey = this.number + "_" + this.fileID;
		boolean backupDone = false;
		
		if(this.peer.getMetadataManager().getChunksHosts().get(hashmapKey) != null) {
			int actualReplicationDegree = this.peer.getMetadataManager().getActualReplicationDegrees().get(hashmapKey);
			
			if(actualReplicationDegree >= this.replicationDegree) {					
				backupDone = true;
			} 	
		} 
		
		//If the desired replication degree has not been fulfilled it sends again the putchunk request
		if(!backupDone) {
			byte [] packet = makePutChunkRequest();
			this.peer.sendReplyToPeers(Peer.channelType.MDB, packet);
		}
		
		return backupDone;
	};
	
	private byte[] makePutChunkRequest() {
		String message = "PUTCHUNK" + " " + this.peer.getProtocolVersion() + " " +this.peer.getID() + " " + this.fileID + " " + this.number +
				" " + this.replicationDegree + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		byte [] header = message.getBytes();
		byte[] packet = new byte[header.length + this.content.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(this.content, 0, packet, header.length, this.content.length);
		
		return packet;
	}

}
