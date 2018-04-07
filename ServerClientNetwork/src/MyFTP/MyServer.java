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
	private static String[] commandarr = null;

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
					if (message.startsWith("send"))
					{
						command = message;
						bw.write("end\n");
						bw.flush();
						break;
					}
					System.out
							.println("SERVER: Client said \"" + message + "\"");
					if (message.equals("end"))
					{
						break;
					}
				}
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
			}
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendfile(String filename)
	{
		try
		{
			System.out.println("SERVER: Sending " + filename);
			File file = new File(filename);
			byte[] packet = new byte[(int) file.length()];
			FileInputStream fin = new FileInputStream(file);
			BufferedInputStream bin = new BufferedInputStream(fin);
			bin.read(packet, 0, (int) file.length());
			ObjectInputStream inputStream = null;
			ObjectOutputStream oout = new ObjectOutputStream(
					server.getOutputStream());
			header sendobject = new header(packet, 0);
			oout.writeObject(sendobject);
			oout.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		System.out.println(filename + " Sent Successfully to "
				+ server.getRemoteSocketAddress());
	}

	public void receivefile(String filename)
	{
		try
		{
			os = server.getOutputStream();
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);

			byte[] contents = new byte[packetsize];
			FileOutputStream fout = new FileOutputStream(
					"received " + filename);
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			InputStream is = server.getInputStream();

			int bytesRead = 0;

			while ((bytesRead = is.read(contents)) != -1)
			{
				bout.write(contents, 0, bytesRead);
				bw.write("ACK#" + bytesRead + "\n");
				bw.flush();
			}
			bout.flush();
			server.close();

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
				if (command != null)
				{
					commandarr = command.split(" ", 2);
					if ((commandarr[0]).equals("receive"))
					{
						System.out.println("SERVER: Going to receive file.");
						testserver.receivefile(commandarr[1]);
					}
					if ((commandarr[0]).equals("send"))
					{
						System.out.println("SERVER: Got command to " + command);
						testserver.sendfile(commandarr[1]);
						break;
					}
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
