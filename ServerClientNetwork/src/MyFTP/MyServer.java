package MyFTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class MyServer extends Socket
{

	// SERVER OBJECTS
	private static ServerSocket listener = null;
	private static Socket server = null;

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

	public MyServer(int port)
	{
		try
		{
			listener = new ServerSocket(port);
			System.out.println("SERVER: Listening for clients on port "
					+ listener.getLocalPort());
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
			is = server.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			os = server.getOutputStream();
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);
			
			while (true)
			{
				if ((message = br.readLine()) != null)
				{
					if(message.startsWith("send"))
					{
						command = message;
						bw.write("end\n");
						bw.flush();
						break;
					}
					System.out
							.println("SERVER: Client said \"" + message + "\"");
				}
				if (message.equals("end"))
				{
					break;
				}
				message = scanner.nextLine();
				bw.write(message + "\n");
				bw.flush();
			}
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public static void main(String args[])
	{
		int port;

		scanner = new Scanner(System.in);
		System.out.println("SERVER: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());

		MyServer testserver = new MyServer(port);
		while (true)
		{
			try
			{
				server = listener.accept();
				System.out.println("SERVER: connected to "
						+ server.getRemoteSocketAddress());
				testserver.messageexchange();
				System.out.println("SERVER: back in main");
				if(command!=null)
				{
					System.out.println("Got command to "+command);
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
