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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
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

	// VARIABLES
	private int packetsize = 1000;
	private static int port;
	private static String message = null;
	private static String filename = null;
	private static String serveraddress = null;
	private static String command = null;
	private static String[] commandarr = null;
	private static byte[] contents = null;
	private static byte[] packet = null;
	private static long filelen = 0;
	private long sequence = 0;
	private int size = 0;
	private int bytesRead = 0;
	private int ack = 0;

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

	private void messageexchange() throws IOException
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

	private void sendfile(File file) throws IOException
	{
		System.out.println("CLIENT: Sending " + filename);

		filelen = file.length();

		while (sequence != filelen)
		{
			size = packetsize;
			if (filelen - sequence >= size)
			{
				sequence += size;
			}
			else
			{
				size = (int) (filelen - sequence);
				sequence = filelen;
			}
			packet = new byte[size];
			bin.read(packet, 0, size);
			os.write(packet);
			if ((message = br.readLine()) != null)
			{
				System.out.println("CLIENT: From Server \"" + message + "\"");
			}
		}
		System.out.println(filename + " Sent Successfully to "
				+ client.getRemoteSocketAddress());
	}

	private void receivefile() throws IOException
	{
		contents = new byte[packetsize];

		while ((bytesRead = is.read(contents)) != -1)
		{
			ack += bytesRead;
			bout.write(contents, 0, bytesRead);
			bw.write("ACK#" + ack + "\n");
			bw.flush();
		}
		bout.flush();
		client.close();

		System.out.println("received "+filename +" saved successfully!");
		bout.close();
	}

	public static void main(String args[]) throws IOException
	{
		System.out.println("CLIENT: Enter Server Address:");
		serveraddress = scanner.nextLine();
		System.out.println("CLIENT: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());

		MyClient testclient = new MyClient(serveraddress, port);

		testclient.messageexchange();

		System.out.println("CLIENT: back in main");

		if (command != null)
		{
			commandarr = command.split(" ", 2);
			filename = commandarr[1];

			if ((commandarr[0]).equals("receive"))
			{
				System.out.println("CLIENT: Going to receive file.");
				fout = new FileOutputStream("received " + filename);
				bout = new BufferedOutputStream(fout);
				testclient.receivefile();
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
