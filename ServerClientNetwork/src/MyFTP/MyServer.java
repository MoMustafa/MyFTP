package MyFTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MyServer extends Socket
{
	// SERVER OBJECTS
	private static ServerSocket listener = null;
	private static Socket server = null;

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
	private static String command = null;
	private static String[] commandarr = null;

	public MyServer(int port) throws IOException
	{
		listener = new ServerSocket(port);
		System.out.println("SERVER: Listening for clients on port "
				+ listener.getLocalPort());
		server = listener.accept();
		System.out.println(
				"SERVER: connected to " + server.getRemoteSocketAddress());
		is = server.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);
		os = server.getOutputStream();
		osw = new OutputStreamWriter(os);
		bw = new BufferedWriter(osw);
	}

	
	public void communicate() throws IOException
	{
		while (true)
		{
			if ((message = br.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					command = message;
					bw.write("end\n");
					bw.flush();
					break;
				}
				System.out.println("SERVER: Client said \"" + message + "\"");
				if (message.equals("end")) break;
			}
			message = scanner.nextLine();
			if (message.startsWith("send"))
			{
				String filename = (message.split(" ", 2))[1];
				command = "receive " + filename;
			}
			bw.write(message + "\n");
			bw.flush();
			if (message.equals("end")) break;
		}
	}

	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("SERVER: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());
		MyServer testserver = new MyServer(port);

		while (true)
		{
			testserver.communicate();
			if (command != null)
			{
				commandarr = command.split(" ", 2);
				filename = commandarr[1];

				if ((commandarr[0]).equals("receive"))
				{
					System.out.println("SERVER: Going to receive file.");
					new TCP();
					TCP.receivefileTCP(filename, server);
					return;
				}
				if ((commandarr[0]).equals("send"))
				{
					File file = new File(filename);
					if (!file.exists())
					{
						System.out.println(
								"SERVER: " + filename + " does not exist.");
						bw.write(filename + " does not exist.");
						bw.flush();
						return;
					}
					System.out.println("SERVER: Got command to " + command);
					new TCP();
					TCP.sendfileTCP(filename, server);
					os.flush();
					server.close();
					break;
				}
			}
			testserver.close();
		}
	}
}
