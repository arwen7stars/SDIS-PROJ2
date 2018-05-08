package main;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastListenner implements Runnable {
	private Peer peer;
	private MulticastSocket socket;
	
	public MulticastListenner(InetAddress address, int port, Peer peer) throws IOException {
		this.peer = peer;
		
		this.socket = new MulticastSocket(port);
		this.socket.joinGroup(address);
	}

	@Override
	public void run() {		
		while(true) {
			byte[] requestPacket = new byte[64500];
			DatagramPacket packet = new DatagramPacket(requestPacket, requestPacket.length);

			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread(new EventHandler(packet, this.peer)).start();
		}
	}

}
