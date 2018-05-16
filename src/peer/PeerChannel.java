package peer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class PeerChannel implements Runnable {
	private Peer peer;
	private DatagramSocket socket;
	
	public PeerChannel(Peer peer) throws IOException {
		this.peer = peer;
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void run() {		
		while(true) {
			byte[] requestPacket = new byte[64500];
			DatagramPacket packet = new DatagramPacket(requestPacket, requestPacket.length);
			
			//System.out.println("Peer socket " + peer.getServerID() + " listening to messages in 'multicast' channel...");

			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread(new EventHandler(packet, this.peer)).start();
		}
	}
	
	public int getPort()
	{
		return socket.getLocalPort();		// host is localhost
	}

}
