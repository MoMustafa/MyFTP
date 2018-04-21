package MyFTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
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

	// VARIABLES 
	private static int port;
	private static String message = null;
	private static String filename = null;
	private static String serveraddress = null;
	private static String command = null;
	private static String[] commandarr = null;

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
			commandarr = command.split(" ", 3);
			filename = commandarr[1];
                        String protocol = commandarr[2];

			if ((commandarr[0]).equals("receive"))
			{
				System.out.println("CLIENT: Going to receive file.");
				if(protocol.equals("TCP"))
                                {
                                    new TCP();
                                    TCP.receivefileTCP(filename, client);
                                    testclient.close();
                                }
                                else if(protocol.equals("UDP"))
                                {
                                    new UDP(port);
                                  UDP.sendDataUDP(filename);
                                    testclient.close();
                                }
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
				System.out.println("CLIENT: Got command to " + command);
				new TCP();
				TCP.sendfileTCP(filename, client);
				os.flush();
				client.close();
				testclient.close();
				return;
			}
			testclient.close();
		}
	}
}
