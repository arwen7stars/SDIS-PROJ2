package RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;


public class RMI
{
    
	public static void main(String[] args)
	{		
		try
		{
			LocateRegistry.createRegistry(1099);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		System.out.println("RMI running");
		
		while(true)
		{
			
		}
	}

} 