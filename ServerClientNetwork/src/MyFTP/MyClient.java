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

	/*
	 * Instantiates MyClient by using the user assigned port number and server
	 * address. I/O Streams are also instantiated so that They are not
	 * prematurely closed elsewhere in the code.
	 */
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

	/*
	 * Begins communication between server and client. On this client side, the
	 * Client Socket initiates the communication, after which each side takes
	 * turns writing replies. If any side sends "end" both sides begin to
	 * terminate connection. If the client receives a message that begins with
	 * "send" it exits communication mode And goes into sendfile() code.
	 * 
	 * If the server side sends a message that begins with "send" it exits
	 * communication mode And goes into receivefile() mode. In both of those
	 * cases, before exiting, we save the filename being requested or sent.
	 * 
	 * If the file does not exist, the client sends a reply saying so and waits
	 * for the server to send a reply.
	 */
	private void communicate() throws IOException
	{
		boolean DNE = false;

		while (true)
		{
			if (!DNE)
			{
				message = scanner.nextLine();
				if (message.startsWith("send"))
				{
					String filename = (message.split(" ", 2))[1];
					command = "receive " + filename;
				}
				bw.write(message + "\n");
				bw.flush();
			}
			DNE = false;
			if (message.equals("end")) break;

			if ((message = br.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					File file = new File((message.split(" ", 2))[1]);
					if (!file.exists())
					{
						System.out.println(
								"CLIENT: Server said \"" + message + "\"");
						System.out.println(
								"CLIENT: Telling Server file does not exist.");
						bw.write("File does not exist. Try again.\n");
						bw.flush();
						DNE = true;
					}
					else
					{
						command = message;
						bw.write("end\n");
						bw.flush();
						break;
					}
				}
				if (!DNE)
					System.out
							.println("CLIENT: Server said \"" + message + "\"");
				if (message.equals("end")) break;
			}
		}
	}

	/*
	 * Main method requests port number and server address from user and creates
	 * a MyClient object. Then the client goes into communicate() mode. When the
	 * client exits the communicate mode, we check the command string to
	 * Determine whether we need to send or receive, and what the filename is We
	 * send the socket and filename as parameters to the appropriate methods.
	 */
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
				new TCP();
				TCP.receivefileTCP(filename, client);
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
				System.out.println("CLIENT: Got command to " + command);
				new TCP();
				TCP.sendfileTCP(filename, client);

				testclient.close();
				return;
			}
			testclient.close();
		}
	}
}
