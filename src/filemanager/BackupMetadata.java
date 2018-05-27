package filemanager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import peer.Peer;

public class BackupMetadata implements Runnable {
	
	private static final int BACKUP_INTERVAL = 30; //seconds
	private Peer peer;
	
	public BackupMetadata(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		while(true) {
			// Schedule task to send metadata to server			
			ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
			Future<Boolean> future = scheduledPool.schedule(sendMetadata, BACKUP_INTERVAL, TimeUnit.SECONDS);
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {}
		}		
	}
	
	Callable<Boolean> sendMetadata = () -> {				
        File file = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.METADATA_FILE);
        
        if(file.exists())  {
            try   {
            	// Send metadata file to Server
	        	byte [] bytes  = new byte [(int)file.length()];
	        	FileInputStream fis = new FileInputStream(file);
	        	BufferedInputStream bis = new BufferedInputStream(fis);
	        	bis.read(bytes, 0, bytes.length);
	        	
	        	bis.close();
	        	fis.close();
	        	
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				System.out.println("Sending metadata to server ("+ bytes.length + " bytes): "+ dateFormat.format(date));
	        	
	        	peer.getServerChannel().sendMessage("SAVE_METADATA");
	        	peer.getServerChannel().sendBytes(bytes);   
	        	
	        	return true;
            }   catch (IOException e)   {
            	System.out.println("Problem sending metadata to server");
                return false;
            }
        }
		return false;
	};
}
