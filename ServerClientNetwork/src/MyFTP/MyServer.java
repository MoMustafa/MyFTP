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
				}
				if (message.equals("end"))
				{
					break;
				}
				message = scanner.nextLine();
				if (message.startsWith("send"))
				{
					command = "receive";
				}
				bw.write(message + "\n");
				bw.flush();
			}
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendfile(String filename)
	{
		System.out.println("SERVER: Sending " + filename);
		try
		{
			File file = new File(filename);
			if (!file.exists())
			{
				System.out.println("File does not exist)");
				OutputStream out = server.getOutputStream();
				OutputStreamWriter ow = new OutputStreamWriter(out);
				BufferedWriter bw = new BufferedWriter(ow);
				bw.write(filename + " does not exist.");
				bw.flush();
				return;
			}
			FileInputStream fin = new FileInputStream(file);
			BufferedInputStream bin = new BufferedInputStream(fin);
			OutputStream os = server.getOutputStream();

			byte[] packet;
			long filelen = file.length();
			long sequence = 0;

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
				os.write(packet);
				System.out.println("SERVER: ACK #" + sequence);
			}
			os.flush();
			server.close();
			listener.close();
			bin.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		System.out.println(filename + " Sent Successfully to "
				+ server.getRemoteSocketAddress());

	}

	public void receivefile()
	{
		try
		{
			byte[] contents = new byte[packetsize];
			FileOutputStream fout = new FileOutputStream("received.jpg");
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			InputStream is = server.getInputStream();

			int bytesRead = 0;

			while ((bytesRead = is.read(contents)) != -1)
				bout.write(contents, 0, bytesRead);

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
					if((commandarr[0]).equals("receive"))
					{
						System.out.println("SERVER: Going to receive file.");
						testserver.receivefile();
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
