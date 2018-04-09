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
import java.util.Scanner;

public class MyClient extends Socket
{
	// SERVER OBJECTS
	private static Socket client = null;

	// I/O OBJECTS
	private static Scanner scanner = new Scanner(System.in);
	private static InputStream is;
	private static InputStreamReader isr;
	private static OutputStream os;
	private static OutputStreamWriter osw;
	private static BufferedReader br;
	private static BufferedWriter bw;
	private static FileInputStream fin;
	private static BufferedInputStream bin;
	private static FileOutputStream fout;
	private static BufferedOutputStream bout;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;

	// VARIABLES
	private static int port;
	private final int packetsize = 1000;
	private static String testchecksum = null;
	private static String checksum = null;
	private static String message = null;
	private static String filename = null;
	private static String serveraddress = null;
	private static String command = null;
	private static String[] commandarr = null;
	private static byte[] packet = null;
	private static long filelen = 0;
	private long sequence = 0;
	private long endsequence = 0;
	private int size = 0;
	private int bytesRead = 0;
	private long ack = 0;
	private long starttime = 0;
	private long timeout = 1000000;

	// CONTAINERS
	private header testheader = null;
	private ArrayList<header> buffer = null;

	Random rand = new Random();

	public MyClient(String serveraddress, int port) throws IOException
	{
		System.out.println(
				"CLIENT: connecting to " + serveraddress + " on port " + port);
		client = new Socket(serveraddress, port);
		System.out.println(
				"CLIENT: connected to " + client.getRemoteSocketAddress());
		is = client.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);
		os = client.getOutputStream();
		osw = new OutputStreamWriter(os);
		bw = new BufferedWriter(osw);
	}

	private void communicate() throws IOException
	{
		while (true)
		{
			message = scanner.nextLine();
			if (message.startsWith("send"))
			{
				String filename = (message.split(" ", 2))[1];
				command = "receive " + filename;
			}
			bw.write(message + "\n");
			bw.flush();

			if (message.equals("end")) break;

			if ((message = br.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					command = message;
					bw.write("end\n");
					bw.flush();
					break;
				}
				System.out.println("CLIENT: Server said \"" + message + "\"");
				if (message.equals("end")) break;
			}
		}
	}

	private void sendfile(File file)
			throws IOException, NoSuchAlgorithmException
	{
		System.out.println("CLIENT: Sending " + filename);

		new ChecksumGen();
		checksum = ChecksumGen.getChecksum(filename);

		filelen = file.length();
		sequence = (long) rand.nextInt(Integer.SIZE - 1) + 1;
		endsequence = sequence + filelen;

		buffer = new ArrayList<header>();

		starttime = System.nanoTime();

		while (sequence != endsequence)
		{
			size = packetsize;
			if (endsequence - sequence >= size)
			{
				sequence += size;
			}
			else
			{
				size = (int) (endsequence - sequence);
				sequence = endsequence;
			}
			packet = new byte[size];
			bin.read(packet, 0, size);
			testheader = new header(packet, sequence, checksum);

			buffer.add(testheader);

			System.out.println("CLIENT: Sending packet #" + testheader.num);
			oos.writeObject(testheader);
			if ((message = br.readLine()) != null)
			{
				System.out.println(
						"\t\t\t\t\tCLIENT: From SERVER \"" + message + "\"");
				timeout();
			}
		}
		System.out.println("CLIENT: " + filename + " Sent Successfully to "
				+ client.getRemoteSocketAddress());
		oos.writeObject(null);
		if ((message = br.readLine()) != null)
		{
			System.out.println("CLIENT: From SERVER \"" + message + "\"");
		}
		buffer.clear();
	}

	private void receivefile()
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		int drop = 0;
		buffer = new ArrayList<header>();
		while ((testheader = (header) ois.readObject()) != null)
		{
			drop++;

			testchecksum = testheader.checksum;
			bytesRead = testheader.payload.length;

			if (drop != 3)
			{
				System.out
						.println("CLIENT: Received packet #" + testheader.num);
				buffer.add(testheader);
				Collections.sort(buffer, header.compareheader);
				if (testheader.num == ack || ack == 0)
					ack = buffer.get(buffer.size() - 1).num + packetsize;
				System.out.println(
						"\t\t\t\t\tCLIENT: Sending to SERVER ACK " + ack);
			}
			bw.write("ACK " + ack + "\n");
			bw.flush();
		}
		bout.flush();
		for (header testheader : buffer)
		{
			bout.write(testheader.payload, 0, testheader.payload.length);
		}
		bout.close();
		buffer.clear();

		new ChecksumGen();
		checksum = ChecksumGen.getChecksum("received " + filename);

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
		client.close();
		System.out.println(
				"CLIENT: \"received " + filename + "\" saved successfully!");
		bout.close();
	}

	public void timeout() throws IOException
	{
		if ((System.nanoTime() - starttime) > timeout)
		{
			System.out.println("\t\t\tCLIENT: TIMEOUT\n");
			starttime = System.nanoTime();

			commandarr = message.split(" ", 2);

			ack = Long.parseLong(commandarr[1]);
			if ((ack - testheader.num) < 0)
			{
				for (header t : buffer)
				{
					if (t.num == ack)
					{
						System.out.println(
								"CLIENT: Retransmitting packet # " + ack);
						oos.writeObject(t);
						if ((message = br.readLine()) != null)
						{
							System.out
									.println("\t\t\t\t\tCLIENT: From SERVER \""
											+ message + "\"");
						}
					}
				}
			}
		}
		else
			return;
	}

	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("CLIENT: Enter Server Address:");
		serveraddress = scanner.nextLine();
		System.out.println("CLIENT: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());

		MyClient testclient = new MyClient(serveraddress, port);

		testclient.communicate();
		if (command != null)
		{
			commandarr = command.split(" ", 2);
			filename = commandarr[1];

			if ((commandarr[0]).equals("receive"))
			{
				System.out.println("CLIENT: Going to receive file.");
				fout = new FileOutputStream("received " + filename);
				bout = new BufferedOutputStream(fout);
				ois = new ObjectInputStream(is);
				testclient.receivefile();
				testclient.close();
				return;
			}
			if ((commandarr[0]).equals("send"))
			{
				File file = new File(filename);
				if (!file.exists())
				{
					System.out.println(
							"CLIENT: " + filename + " does not exist.");
					bw.write(filename + " does not exist.");
					bw.flush();
					testclient.close();
					return;
				}

				fin = new FileInputStream(file);
				bin = new BufferedInputStream(fin);
				oos = new ObjectOutputStream(os);
				System.out.println("CLIENT: Got command to " + command);
				testclient.sendfile(file);
				os.flush();
				bin.close();
				client.close();
				return;
			}
			testclient.close();
		}
	}
}
