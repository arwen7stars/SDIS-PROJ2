package peer;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutionException;
import java.io.IOException;

public class RunPeer {
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		if(args.length != 2){
			System.out.println("Wrong number of args.");
			System.exit(1);
		}
		int serverID = Integer.valueOf(args[0]);
		String hostIP = args[1];

		String rmiAddress = "peer"+serverID;

		Peer peer = new Peer("1.0", serverID, hostIP);
		
		// Start RMI - Client Connection 
		try
		{
		    IRMI rmi = (IRMI) UnicastRemoteObject.exportObject((Remote) peer, 0);
		    Registry registry = LocateRegistry.getRegistry();
		    registry.rebind(rmiAddress, rmi);
		}
		catch (RemoteException e)
		{
		    e.printStackTrace();
		}
	}
}
