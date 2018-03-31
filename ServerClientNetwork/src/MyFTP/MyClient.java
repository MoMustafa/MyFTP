package MyFTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

	public void messageexchange()
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
				bw.write(message + "\n");
				bw.flush();
				if ((message = br.readLine()) != null)
				{
					System.out
							.println("CLIENT: Server said \"" + message + "\"");
				}
				if (message.equals("end"))
				{
					break;
				}
			}

			bw.close();
			br.close();
			osw.close();
			isr.close();
			os.close();
			is.close();
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void tcpfileexchange()
	{
		return;
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

	}
}
