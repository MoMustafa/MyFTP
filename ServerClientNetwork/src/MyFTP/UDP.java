package MyFTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class UDP
{
	private static DatagramSocket udpSocket;
	private static InetAddress address;
	private static int port;
	private static int packetsize = 1000;

	public UDP(int p, InetAddress addr) throws SocketException
	{
		address = addr;
		port = p;
	}

	public static void receive(String filename) throws IOException
	{
		udpSocket = new DatagramSocket(port);
		
		FileOutputStream fout = new FileOutputStream("received " + filename);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		
		ArrayList <byte[]> tempbuffer = new ArrayList <byte[]>();
		
		while (true)
		{
			byte[] buffer = new byte[packetsize];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			udpSocket.receive(packet);
			System.out.println("received data");
			
			String quote = new String(buffer, 0, packet.getLength());
			if(quote.equals("end"))
				break;
			
			tempbuffer.add(packet.getData());
		}
		
		for(int i = 0; i<tempbuffer.size(); i++)
			bout.write(tempbuffer.get(i), 0, tempbuffer.get(i).length);
		
		System.out.println("done");
	}

	public static void send(String filename) throws IOException
	{
		udpSocket = new DatagramSocket();
		
		File file = new File(filename);
		FileInputStream fin = new FileInputStream(file);
		BufferedInputStream bin = new BufferedInputStream(fin);

		long filelen = file.length();
		long current = 0;

		while (current != filelen)
		{
			int size = packetsize;
			if (filelen - current >= size)
				current += size;
			else
			{
				size = (int) (filelen - current);
				current = filelen;
			}
			byte[] packet = new byte[size];
			bin.read(packet, 0, size);
			DatagramPacket p = new DatagramPacket(packet, size, address, port);
			udpSocket.send(p);
			System.out.println("sent packet");
		}
		String end = "end";
		udpSocket.send(new DatagramPacket(end.getBytes(), end.length(), address, port));
		bin.close();
	}
}
