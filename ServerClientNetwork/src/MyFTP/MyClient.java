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
import java.util.Scanner;

public class MyClient extends Socket
{
	// SERVER OBJECTS
	private static Socket client = null;

	// I/O OBJECTS
	static InputStream is;
	static InputStreamReader isr;
	static OutputStream os;
	static OutputStreamWriter osw;
	static BufferedReader br;
	static BufferedWriter bw;
	static Scanner scanner;

	// VARIABLES
	private int packetsize = 1000;
	private static String command = null;
	private static String[] commandarr = null;

	public MyClient(String serveraddress, int port)
	{
		try
		{
			System.out.println("CLIENT: connecting to " + serveraddress
					+ " on port " + port);
			client = new Socket(serveraddress, port);
			System.out.println(
					"CLIENT: connected to " + client.getRemoteSocketAddress());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void messageexchange()
	{
		try
		{
			String message = null;
			is = client.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			os = client.getOutputStream();
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);

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
				if (message.equals("end"))
				{
					break;
				}
				if ((message = br.readLine()) != null)
				{
					if (message.startsWith("send"))
					{
						command = message;
						bw.write("end\n");
						bw.flush();
						break;
					}
					System.out
							.println("CLIENT: Server said \"" + message + "\"");
					if (message.equals("end"))
					{
						break;
					}
				}
			}
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void sendfile(String filename)
	{
		System.out.println("SERVER: Sending " + filename);
		try
		{
			String message = null;
			is = client.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			os = client.getOutputStream();
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);

			File file = new File(filename);
			if (!file.exists())
			{
				System.out.println("CLIENT: " + filename + " does not exist.");
				bw.write(filename + " does not exist.");
				bw.flush();
				return;
			}
			FileInputStream fin = new FileInputStream(file);
			BufferedInputStream bin = new BufferedInputStream(fin);
			OutputStream fout = client.getOutputStream();

			byte[] packet;
			long filelen = file.length();
			long sequence = 0;

			do
			{
				while (sequence != filelen)
				{
					int size = packetsize;
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
					fout.write(packet);
					if ((message = br.readLine()) != null)
					{
						System.out.println(
								"CLIENT: From Server \"" + message + "\"");
					}
				}
			}while(sequence!=filelen);
			os.flush();
			client.close();
			bin.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println(filename + " Sent Successfully to "
				+ client.getRemoteSocketAddress());
	}

	private void receivefile(String filename)
	{
		try
		{
			os = client.getOutputStream();
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);
			
			byte[] contents = new byte[packetsize];
			FileOutputStream fout = new FileOutputStream(
					"received " + filename);
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			InputStream is = client.getInputStream();
			
			int bytesRead = 0;

			while ((bytesRead = is.read(contents)) != -1)
			{
				bout.write(contents, 0, bytesRead);
				bw.write("ACK#"+bytesRead+"\n");
				bw.flush();
			}
			bout.flush();
			client.close();

			System.out.println("File saved successfully!");
			bout.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String args[])
	{
		int port;
		String serveraddress;

		scanner = new Scanner(System.in);
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
			if ((commandarr[0]).equals("receive"))
			{
				System.out.println("CLIENT: Going to receive file.");
				testclient.receivefile(commandarr[1]);
			}
			if ((commandarr[0]).equals("send"))
			{
				System.out.println("CLIENT: Got command to " + command);
				testclient.sendfile(commandarr[1]);
				return;
			}
			return;
		}
		
	}
}
