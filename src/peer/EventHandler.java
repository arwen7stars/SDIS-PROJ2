package peer;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import filemanager.FileChunk;
import protocols.Delete;

public class EventHandler implements Runnable {
	private Peer peer;
	private String[] header;
	private byte[] body;

	public static final String CRLF = "\r\n";
	public static final byte CR = 0xD;
	public static final byte LF = 0xA;
	
	public EventHandler(DatagramPacket packet, Peer peer) {
		this.peer = peer;
		splitMessage(packet);
	}

	public void splitMessage(DatagramPacket packet) {
		// To remove empty spaces of the packet
		byte[] message = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), message, 0, packet.getLength());

		ByteArrayInputStream input = new ByteArrayInputStream(message);

		byte character = 0;
		String allHeader = "";

		// Read char by char
		while (character != CR && character != -1) {
			character = (byte) input.read();
			allHeader += (char) character;
		}

		// After CR comes LF to end the header message
		character = (byte) input.read();
		if (character != LF) {
			System.out.println("ERROR: Message with wrong header");
			return;
		} else {
			allHeader += (char) character;
		}

		// Length + 2 because of the extra CRLF
		this.body = Arrays.copyOfRange(message, allHeader.length() + 2, message.length);
		this.header = allHeader.trim().split(" ");
	}

	@Override
	public void run() {
		// Check if it was me that sent the message - Ignore
		if (Integer.parseInt(header[2]) == this.peer.getID()) {
			return;
		}

		String hashmapKey = "";
		Random random;
		int waitTime;

		switch (header[0]) {

		case "PUTCHUNK":
			if (header.length != 6) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}

			// Save desired replication degree of chunk file
			hashmapKey = header[4] + "_" + header[3];
			
			// Stores that received the putchunk message - For reclaim protocol
			if (!this.peer.getReceivedPutChunkMessages().contains(hashmapKey)) {
				this.peer.getReceivedPutChunkMessages().add(hashmapKey);
			}
			
			// Peer initiator of backup don't store any chunk of the file
			if(this.peer.getMetadataManager().getBackupState().get(header[3]) != null) {
				return;
			}
			
			this.peer.getMetadataManager().getDesiredReplicationDegrees().put(hashmapKey, Integer.parseInt(header[5]));

			// Check if I already stored this chunk
			CopyOnWriteArrayList<Integer> chunkHosts = peer.getMetadataManager().getChunksHosts().get(hashmapKey);

			if (chunkHosts != null && chunkHosts.contains(this.peer.getID())) {
				return;
			}

			// Check if I have disk space to store the chunk
			if (this.body.length + this.peer.getMetadataManager().getDiskUsed() > this.peer.getMetadataManager().getDiskMaxSpace()) {
				return;
			}

			random = new Random();
			waitTime = random.nextInt(400);

			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

			executor.schedule(storeChunk, waitTime, TimeUnit.MILLISECONDS);

			break;

		case "STORED":
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}

			// Save chunks information
			this.peer.getMetadataManager().storeChunkInfo(Integer.parseInt(this.header[2]), this.header[3],
					Integer.parseInt(this.header[4]));
			this.peer.getMetadataManager().saveMetadata();

			break;

		case "GETCHUNK":
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
		
			hashmapKey = header[4] + "_" + header[3];

			//Ignore If I don't have the chunk stored
			if (this.peer.getMetadataManager().getChunksHosts().get(hashmapKey) != null
					&& !this.peer.getMetadataManager().getChunksHosts().get(hashmapKey).contains(this.peer.getID())) {
				return;
			}

			random = new Random();
			waitTime = random.nextInt(400);

			ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

			Future<Boolean> future = scheduledPool.schedule(receivedChunkMsg, waitTime, TimeUnit.MILLISECONDS);
			boolean chunkAlreadySent = false;
			try {
				chunkAlreadySent = future.get();
			} catch (InterruptedException | ExecutionException e) {
			}

			if (!chunkAlreadySent) {
				byte[] packet = makeChunkMessage(header[3], header[4]);
				try {
					this.peer.sendReplyToPeers(Peer.channelType.MDR, packet);
				} catch (IOException e) {
					System.out.println("Error sending chunk message");
				}
			}

			break;

		case "CHUNK":
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}

			hashmapKey = header[4] + "_" + header[3];

			// Stores that received the chunk message - For restore protocol
			if (!this.peer.getReceivedChunkMessages().contains(hashmapKey)) {
				this.peer.getReceivedChunkMessages().add(hashmapKey);
			}

			// Check if I am waiting for this chunk
			if (this.peer.getWaitRestoredChunks().contains(hashmapKey)) {
				this.peer.getWaitRestoredChunks().remove(hashmapKey);
				this.peer.getRestoredChunks().put(hashmapKey, this.body);
			}

			break;

		case "DELETE":
			if (header.length != 4) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}

			new Thread(new Delete(header[3], this.peer)).start();

			break;

		case "REMOVED":
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			hashmapKey = header[4] + "_" + header[3];
			
			//Remove received putchunk to don't conflit with previous backup
			if (this.peer.getReceivedPutChunkMessages().contains(hashmapKey)) {
				this.peer.getReceivedPutChunkMessages().remove(hashmapKey);
			}
			
			//Update memory chunks info
			this.peer.getMetadataManager().removeChunkInfo(hashmapKey, Integer.parseInt(header[2]));
			this.peer.getMetadataManager().saveMetadata();

			// Check if I have stored this chunk, to see the perceived replication degree
			if (this.peer.getMetadataManager().getChunksStoredSize().get(hashmapKey) != null) {
				int actualReplicationDegree = this.peer.getMetadataManager().getActualReplicationDegrees().get(hashmapKey);
				int desiredReplicationDegree = this.peer.getMetadataManager().getDesiredReplicationDegrees().get(hashmapKey);

				if (actualReplicationDegree < desiredReplicationDegree) {
					reBackupFile(hashmapKey, desiredReplicationDegree);
				}
			}

			break;
		}
	}

	/*private boolean fulfilledRep(String key) {
		if(this.peer.getActualReplicationDegrees().get(key) != null) {
			int desiredRepDegree = this.peer.getDesiredReplicationDegrees().get(key);
			int perceivedRepDegree = this.peer.getActualReplicationDegrees().get(key);
			
			if(perceivedRepDegree >= desiredRepDegree) {
				return true;
			}
		}
		
		return false;
	}*/

	private void reBackupFile(String hashmapKey, int replication) {
		Random random = new Random();
		int waitTime = random.nextInt(400);

		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		Future<Boolean> future = scheduledPool.schedule(receivedPutChunkMsg, waitTime, TimeUnit.MILLISECONDS);
		boolean chunkBackedUp = false;
		try {
			chunkBackedUp = future.get();
		} catch (InterruptedException | ExecutionException e) {
		}

		if (!chunkBackedUp) {
			byte[] packet = makePutChunkRequest(header[3], header[4], replication);
			try {
				this.peer.sendReplyToPeers(Peer.channelType.MDR, packet);
			} catch (IOException e) {
				System.out.println("Error sending chunk message");
			}
		}
	}
	
	private byte[] makePutChunkRequest(String fileID, String chunkNr, int replicationDegree) {
		String message = "PUTCHUNK" + " " + this.peer.getProtocolVersion() + " " +this.peer.getID() + " " + fileID + " " + chunkNr +
				" " + replicationDegree + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		byte [] chunk = FileChunk.getChunk(peer.getID(), fileID, chunkNr);
		
		byte [] header = message.getBytes();
		byte[] packet = new byte[header.length + chunk.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(chunk, 0, packet, header.length, chunk.length);
		
		return packet;
	}

	private byte[] makeStoreChunkReply(String fileID, String chunkNr) {
		String message = "STORED " + this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID + " "
				+ chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;

		return message.getBytes();
	}

	private byte[] makeChunkMessage(String fileID, String chunkNr) {
		byte[] chunk = FileChunk.getChunk(peer.getID(), fileID, chunkNr);

		String message = "CHUNK " + this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID + " "
				+ chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;

		byte[] header = message.getBytes();
		byte[] packet = new byte[header.length + chunk.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(chunk, 0, packet, header.length, chunk.length);

		return packet;
	}

	
	Runnable storeChunk = () -> {
		
		
		String filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.CHUNKS_FOLDER + "/"
				+ this.header[4] + "_" + this.header[3];

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(this.body);
		} catch (IOException e) {
			System.out.println("Error saving chunk file");
		}

		
		// Save chunks information
		this.peer.getMetadataManager().getChunksStoredSize().put(this.header[4] + "_" + this.header[3], this.body.length);
		this.peer.getMetadataManager().storeChunkInfo(this.peer.getID(), this.header[3], Integer.parseInt(this.header[4]));
		
		// Update disk usage
		this.peer.getMetadataManager().setDiskUsed(this.peer.getMetadataManager().getDiskUsed() + this.body.length);
		
		//Save non volatile memory
		this.peer.getMetadataManager().saveMetadata();

		// Send message STORED
		byte[] packet = makeStoreChunkReply(this.header[3], this.header[4]);
		try {
			this.peer.sendReplyToPeers(Peer.channelType.MC, packet);
		} catch (IOException e) {
			System.out.println("Error sending message to multicast");
		}
	};

	Callable<Boolean> receivedChunkMsg = () -> {
		boolean result = false;
		String key = this.header[4] + "_" + this.header[3];

		if (this.peer.getReceivedChunkMessages().contains(key)) {
			result = true;
		}

		return result;
	};
	
	Callable<Boolean> receivedPutChunkMsg = () -> {
		boolean result = false;
		String key = this.header[4] + "_" + this.header[3];

		if (this.peer.getReceivedPutChunkMessages().contains(key)) {
			result = true;
		}

		return result;
	};
}
