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
import java.util.Scanner;

public class UDP
{
	private static DatagramSocket udpSocket;
	private static int port;
	private static int packetsize = 1000;

	public UDP(int p) throws SocketException
	{
		port = p;
	}

	public static void receive(String filename) throws IOException
	{
		udpSocket = new DatagramSocket(port);
		System.out.println("IP Address of Receiver: "+InetAddress.getLocalHost());
		FileOutputStream fout = new FileOutputStream("received " + filename);
		BufferedOutputStream bout = new BufferedOutputStream(fout);

		long starttime = System.nanoTime();
		while (true)
		{
			if ((System.nanoTime() - starttime) > 1000000)
				return;
			byte[] buffer = new byte[packetsize];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			udpSocket.receive(packet);
			System.out.println("received data");
			bout.write(packet.getData(), 0, packet.getData().length);

			String quote = new String(buffer, 0, packet.getLength());
		}
	}

	public static void send(String filename, InetAddress address)
			throws IOException
	{
		Scanner scanner = new Scanner(System.in);
		udpSocket = new DatagramSocket();
		System.out.println("Enter destination IP: ");
		String addr = scanner.nextLine();
		address = InetAddress.getByName(addr);
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
		udpSocket.send(new DatagramPacket(end.getBytes(), end.length(), address,
				port));
		System.out.println("Ending");
		bin.close();
	}
}
