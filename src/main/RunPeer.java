package main;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;

public class RunPeer {
	
	public static void main(String[] args) throws IOException {
		if(args.length != 1){
			System.out.println("Wrong number of args.");
			System.exit(1);
		}
		int serverID = Integer.valueOf(args[0]);

		String rmiAddress = "rmi"+serverID;

		Peer peer = new Peer("1.0", serverID);
		
		// Start RMI - Client Connection 
		/*try
		{
		    IRMI rmi = (IRMI) UnicastRemoteObject.exportObject((Remote) peer, 0);
		    Registry registry = LocateRegistry.getRegistry();
		    registry.rebind(rmiAddress, rmi);
		}
		catch (RemoteException e)
		{
		    e.printStackTrace();
		}*/
	
	}
}