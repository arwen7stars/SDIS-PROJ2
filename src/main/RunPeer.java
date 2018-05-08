package main;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import java.net.InetAddress;

public class RunPeer {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 9) {
            System.out.println("Wrong arguments inserted.\nUse the following command:"
            		+ "'java RunPeer <ProtocolVersion> <PeerID>  <AccessPoint> <MCAddress> <MCPort> <MDBAddress> <MDBPort> <MDRAddress> <MDRPort>'");
            return;
        }
		
		String protocolVersion = args[0];
		int serverID = Integer.parseInt(args[1]);
		String rmiAddress = args[2]; 
		InetAddress addressMC = InetAddress.getByName(args[3]);
		int mcPort = Integer.parseInt(args[4]);
		InetAddress addressMDB = InetAddress.getByName(args[5]);
		int mdbPort = Integer.parseInt(args[6]);
		InetAddress addressMDR = InetAddress.getByName(args[7]);
		int mdrPort = Integer.parseInt(args[8]);;
		
		/*int serverID = Integer.parseInt(args[0]);		
		InetAddress addressMC = InetAddress.getByName("224.0.0.2");
		InetAddress addressMDB = InetAddress.getByName("224.0.0.3");
		InetAddress addressMDR = InetAddress.getByName("224.0.0.4");
		String protocolVersion = "1.0";
		int mcPort = 4446, mdbPort = 4446, mdrPort = 4446;
		

		String rmiAddress = "rmi"+serverID; */

		Peer peer = new Peer(protocolVersion, serverID, addressMC, mcPort, addressMDB, mdbPort, addressMDR, mdrPort);
		
		//Start RMI - Client Connection 
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