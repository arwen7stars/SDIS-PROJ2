package protocols;

import peer.Peer;

public class State {
	
	private Peer peer;
	private String state;
	
	public State(Peer peer) {
		this.peer = peer;
		this.state = "";
		
		getInfoStateBackup();
		getInfoStateChunks();
		getInfoStateDisk();
	}
	
	private void getInfoStateBackup() {
		this.state += "\n--- Files whose backup has initiated ---";
		
		if(this.peer.getMetadataManager().getFilesIdentifiers().size() == 0) {
			this.state += "\nNo backup has been started.\n";
		} else {
			this.peer.getMetadataManager().getFilesIdentifiers().forEach( (filename, fileid) -> {
				this.state += "\n- File Path: " + Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_FOLDER + "/" + filename;
				this.state += "\n- File ID: "+ fileid + " - Desired Replication Degree: " + this.peer.getMetadataManager().getDesiredReplicationDegrees().get("0_"+fileid);
				
				if(this.peer.getMetadataManager().getBackupState().get(fileid) == false) {
					this.state += "\nThis file started a backup, but this backup was later deleted.";
				} else {
					this.state += "\nChunks of the file:";
					
					this.peer.getMetadataManager().getActualReplicationDegrees().forEach( (key, perseived) -> {
						if(key.endsWith(fileid)) {
							String [] info = key.split("_");
							this.state += "\nChunkNr: "+info[0] + " - Perceived Replication Degree: "+perseived;
						}
					});
				}
				
				this.state += "\n";
			});
		}
	}
	
	private void getInfoStateChunks() {
		this.state += "\n--- Stored Chunks ---";
		
		if(this.peer.getMetadataManager().getChunksStoredSize().size() == 0) {
			this.state += "\nNo chunks stored.";
		} else {
			this.peer.getMetadataManager().getChunksStoredSize().forEach( (key, size) -> {
				this.peer.getMetadataManager().getActualReplicationDegrees().forEach( (key2, perseived) -> {
					if(key2.equals(key)) {
						this.state += "\n- Chunk ID: "+ key + "\n- Size: "+ size/1000 + " KBytes" + " - Perceived Replication Degree: "+perseived;
					}
				});
			});
		}
		
		this.state += "\n";
	}
	
	private void getInfoStateDisk() {
		this.state += "\n--- Disk Info ---";
		this.state += "\n- Disk Space: "+this.peer.getMetadataManager().getDiskMaxSpace() / 1000 + " KBytes";
		this.state += "\n- Disk Used: "+this.peer.getMetadataManager().getDiskUsed() / 1000 + " KBytes";
		
		this.state += "\n";
	}
	
	public String getState() {
		return this.state;
	}
}
