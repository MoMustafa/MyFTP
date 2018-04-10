package MyFTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TCP
{
	private static final int packetsize = 1000;
	private static final long timeout = 5000000;
	private static long starttime = 0;

	public static void sendfileTCP(String filename, Socket socket)
			throws IOException, NoSuchAlgorithmException
	{

		OutputStream os = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		OutputStreamWriter osw = new OutputStreamWriter(os);
		BufferedWriter bw = new BufferedWriter(osw);

		File file = new File(filename);

		if (!file.exists())
		{
			System.out.println(filename + " does not exist.");
			bw.write(filename + " does not exist.");
			bw.flush();
			return;
		}
		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		FileInputStream fin = new FileInputStream(file);
		BufferedInputStream bin = new BufferedInputStream(fin);

		ArrayList<header> buffer = new ArrayList<header>();
		Random rand = new Random();
		new ChecksumGen();

		String message = null;
		String checksum = ChecksumGen.getChecksum(filename);
		int size = 0;
		long filelen = file.length();
		long sequence = (long) rand.nextInt(Integer.SIZE - 1) + 1;
		long endsequence = sequence + filelen;
		starttime = System.nanoTime();

		System.out.println("Sending " + filename);

		while (sequence != endsequence)
		{
			size = packetsize;
			if (endsequence - sequence >= size)
				sequence += size;
			else
			{
				size = (int) (endsequence - sequence);
				sequence = endsequence;
			}
			byte[] packet = new byte[size];
			bin.read(packet, 0, size);
			header testheader = new header(packet, sequence, checksum);

			buffer.add(testheader);

			System.out.println("Sending packet #" + testheader.num);
			oos.writeObject(testheader);
			if ((message = br.readLine()) != null)
			{
				System.out.println("\t\t\t\t\t\"" + message + "\"");
				timeout(message, testheader.num, buffer, oos, socket);
			}
		}
		System.out.println("\"" + filename + "\" sent successfully to "
				+ socket.getRemoteSocketAddress());
		oos.writeObject(null);
		if ((message = br.readLine()) != null)
			System.out.println("\"" + message + "\"");
		bin.close();
		buffer.clear();
		// remember to check if flush is needed as done in the main method
	}

	public static void receivefileTCP(String filename, Socket socket)
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		FileOutputStream fout = new FileOutputStream("received " + filename);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		InputStream is = socket.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		OutputStream os = socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		BufferedWriter bw = new BufferedWriter(osw);

		header testheader = null;
		ArrayList<header> buffer = new ArrayList<header>();
		String testchecksum = null;

		int drop = 0;
		long ack = 0;
		while ((testheader = (header) ois.readObject()) != null)
		{
			drop++;

			if (drop != 3)
			{
				System.out.println("Received packet #" + testheader.num);
				buffer.add(testheader);
				Collections.sort(buffer, header.compareheader);

				if (testheader.num == ack || ack == 0)
				{
					ack = buffer.get(buffer.size() - 1).num + packetsize;
				}
				System.out.println("\t\t\t\t\t Sending ACK #" + ack);
			}
			bw.write("ACK " + ack + "\n");
			bw.flush();

			testchecksum = testheader.checksum;
		}
		for (header tempheader : buffer)
			bout.write(tempheader.payload, 0, tempheader.payload.length);
		bout.close();
		buffer.clear();

		new ChecksumGen();
		String checksum = ChecksumGen.getChecksum("received " + filename);
		if (checksum.equals(testchecksum))
		{
			bw.write("Checksum matches");
			bw.flush();
		}
		else
		{
			bw.write("Checksum doesn't match");
			bw.flush();
		}
		socket.close();
		System.out
				.println("\"received " + filename + "\" saved successfully from"
						+ socket.getRemoteSocketAddress());
		bout.close();
	}

	public static void timeout(String message, long seq,
			ArrayList<header> buffer, ObjectOutputStream oos, Socket socket)
			throws IOException
	{
		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		if ((System.nanoTime() - starttime) > timeout)
		{
			System.out.println("\t\t\tTIMEOUT\n");
			starttime = System.nanoTime();

			String[] temp = message.split(" ", 2);
			long ack = Long.parseLong(temp[1]);
			if ((ack - seq) < 0)
			{
				for (header t : buffer)
				{
					if (t.num == ack)
					{
						System.out.println("Retransmitting packet #" + t.num);
						oos.writeObject(t);
						if ((message = br.readLine()) != null)
						{
							System.out.println("\t\t\t\t\t\"" + message + "\"");
						}
					}
				}
			}
		}
		else
			return;
	}
}
