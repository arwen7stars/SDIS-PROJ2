package peer; 

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRMI extends Remote
{
    void backup(String filename, int replicationDegree) throws RemoteException;
    
    void delete(String filename) throws RemoteException;
    
    void restore(String filename) throws RemoteException;
    
    void reclaim(int kbytes) throws RemoteException;
    
    String state() throws RemoteException;
}