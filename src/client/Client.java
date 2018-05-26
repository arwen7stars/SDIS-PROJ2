package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import peer.IRMI;

public class Client {
	private static String access_rmi;
	private static String operation;
	private static ArrayList<String> operands;

	public static void main(String args[]) {		
		if(!initArgs(args))
			return;
		
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			IRMI rmi = (IRMI) registry.lookup(access_rmi);
			switch (operation) {
				case "BACKUP":
					try {
						rmi.backup(operands.get(0), Integer.parseInt(operands.get(1)));
					} catch (Exception e) {
						System.err.println("Backup exception:");
						e.printStackTrace();
					}
					break;
					
				case "DELETE":
					try {
						rmi.delete(operands.get(0));
					} catch (Exception e) {
						System.err.println("Delete exception:");
						e.printStackTrace();
					}
					break;
					
				case "RECLAIM":
					try {
						rmi.reclaim(Integer.parseInt(operands.get(0)));
					} catch (Exception e) {
						System.err.println("Reclaim exception:");
						e.printStackTrace();
					}
					break;
					
				case "RESTORE":
					try {
						rmi.restore(operands.get(0));
					} catch (Exception e) {
						System.err.println("Restore exception:");
						e.printStackTrace();
					}
					break;
					
				case "STATE":
					try {
						System.out.println("State of Peer "+access_rmi+":");
						System.out.println(rmi.state());
						//rmi.state();
					} catch (Exception e) {
						System.err.println("State exception:");
						e.printStackTrace();
					}
					break;
	
				default:
					System.err.println("Unknown command");
					break;
			}
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static boolean initArgs(String[] args) {
		access_rmi = args[0];
		operation = args[1];
		operands = new ArrayList<String>();

		if (args.length < 2 || args.length > 4) {
			System.out.println("Invalid usage, wrong number of args");
			return false;
		}

		if (args.length == 3 && operation.equals("DELETE") || operation.equals("RESTORE") || operation.equals("RECLAIM")) {
			operands.add(args[2].trim());
			return true;
		} else if (args.length == 4 && operation.equals("BACKUP")) {
			operands.add(args[2].trim());
			operands.add(args[3].trim());
			return true;
		} else if (args.length == 2 && operation.equals("STATE")){
			return true;
		} else {
			System.out.println("Invalid usage");
			return false;
		}
	}
}
