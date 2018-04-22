package MyFTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MyServer extends Socket
{
	// SERVER OBJECTS
	private static ServerSocket listener = null;
	private static Socket server = null;
	private static InetAddress address = null;

	// I/O OBJECTS
	private static Scanner scanner = new Scanner(System.in);
	private static InputStream is;
	private static InputStreamReader isr;
	private static OutputStream os;
	private static OutputStreamWriter osw;
	private static BufferedReader br;
	private static BufferedWriter bw;

	// VARIABLES//
	private static int port;
	private static String message = null;
	private static String filename = null;
	private static String command = null;
	private static String[] commandarr = null;

	/*
	 * Instantiates MyServer by using the user assigned port number. Also prints
	 * out the server's IP address. ServerSocket Object listens for a connection
	 * by a client at the assigned port then Socket object is returned when the
	 * listener accepts a connection. I/O Streams are also instantiated so that
	 * they are not prematurely closed elsewhere in the code.
	 */
	public MyServer(int port) throws IOException
	{
		address = InetAddress.getLocalHost();
		System.out.println("IP Address: " + address);
		
		while (true)
		{
			listener = new ServerSocket(port);
			System.out.println("SERVER: Listening for clients on port "
					+ listener.getLocalPort());
			server = listener.accept();

			break;
		}
		System.out.println(
				"SERVER: connected to " + server.getRemoteSocketAddress());
		is = server.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);
		os = server.getOutputStream();
		osw = new OutputStreamWriter(os);
		bw = new BufferedWriter(osw);
	}

	/*
	 * Begins communication between server and client. On this server side, the
	 * socket first waits for the client to initiate communication After which
	 * each side takes turns writing replies. If any side sends "end" both sides
	 * begin to terminate connection. If the server receives a message that
	 * begins with "sendUDP" or "sendTCP" it exits communication mode.
	 * 
	 * If the server side sends a message that begins with "sendUDP" or
	 * "sendTCP" it exits communication mode. In both of those cases, before
	 * exiting, we save the filename being requested or sent.
	 * 
	 * If the file does not exist, the server sends a reply saying so and waits
	 * for the client to send a reply.
	 */
	public void communicate() throws IOException
	{
		while (true)
		{
			if ((message = br.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					File file = new File((message.split(" ", 2))[1]);
					if (!file.exists())
					{
						System.out.println(
								"SERVER: Client said \"" + message + "\"");
						System.out.println(
								"SERVER: Telling Client file does not exist.");
						bw.write("File does not exist. Try again\n");
						bw.flush();
						continue;
					}
					else
					{
						command = message;
						bw.write("end\n");
						bw.flush();
						break;
					}
				}
				System.out.println("SERVER: Client said \"" + message + "\"");
				if (message.equals("end")) break;
			}
			message = scanner.nextLine();
			if (message.startsWith("sendTCP"))
			{
				String filename = (message.split(" ", 2))[1];
				command = "receiveTCP " + filename;
			}
			if (message.startsWith("sendUDP"))
			{
				String filename = (message.split(" ", 2))[1];
				command = "receiveUDP " + filename;
			}
			bw.write(message + "\n");
			bw.flush();
			if (message.equals("end")) break;
		}
	}

	/*
	 * Main method requests port number from user and creates a MyServer object.
	 * Then the server immediately goes into communicate() mode. When the server
	 * exits the communicate mode, we check the command string to determine
	 * whether we need to send or receive, what protocol to use, and what the
	 * filename is.
	 */
	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("SERVER: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());
		MyServer testserver = new MyServer(port);

		testserver.communicate();

		if (command != null)
		{
			commandarr = command.split(" ", 2);
			filename = commandarr[1];

			if ((commandarr[0]).equals("receiveTCP")
					|| (commandarr[0]).equals("receiveUDP"))
			{
				System.out.println("SERVER: Going to receive file.");
				if ((commandarr[0]).equals("receiveUDP"))
				{
					new UDP(port);
					UDP.receive(filename);
					testserver.close();
					return;
				}
				if ((commandarr[0]).equals("receiveTCP"))
				{
					new TCP();
					TCP.receive(filename, server);
					testserver.close();
					return;
				}
			}
			if ((commandarr[0]).equals("sendTCP")
					|| (commandarr[0]).equals("sendUDP"))
			{
				File file = new File(filename);
				if (!file.exists())
				{
					System.out.println(
							"SERVER: " + filename + " does not exist.");
					bw.write(filename + " does not exist.");
					bw.flush();

					testserver.close();
					return;
				}
				System.out.println("SERVER: Got command to " + command);
				if ((commandarr[0]).equals("sendUDP"))
				{
					new UDP(port);
					UDP.send(filename, address);
					testserver.close();
					return;
				}
				if ((commandarr[0]).equals("sendTCP"))
				{
					new TCP();
					TCP.send(filename, server);
					testserver.close();
					return;
				}
			}
		}
		testserver.close();
	}
}