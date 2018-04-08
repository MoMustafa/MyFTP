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
import java.util.Random;
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
	private static FileInputStream fin;
	private static BufferedInputStream bin;
	private static FileOutputStream fout;
	private static BufferedOutputStream bout;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	
	// VARIABLES
	private static int port;
	private static int packetsize = 1000;
	private static String message = null;
	private static String filename = null;
	private static String command = null;
	private static String[] commandarr = null;
	private static byte[] packet = null;
	private static long filelen = 0;
	private long sequence = 0;
	private long endsequence = 0;
	private int size = 0;
	private int bytesRead = 0;
	private long ack = 0;
	private header testheader= null;
	
	Random rand = new Random();
	
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

	public void messageexchange() throws IOException
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

	public void sendfile(File file) throws IOException
	{
		System.out.println("CLIENT: Sending " + filename);

		filelen = file.length();
		sequence = (long)rand.nextInt(Integer.SIZE-1)+1;
		System.out.println(sequence);
		endsequence = sequence + filelen;
		
		while (sequence != endsequence)
		{
			size = packetsize;
			if (endsequence - sequence >= size)
			{
				sequence += size;
			}
			else
			{
				size = (int) (endsequence - sequence);
				sequence = endsequence;
			}
			packet = new byte[size];
			bin.read(packet, 0, size);
			testheader = new header(packet, sequence);
			oos.writeObject(testheader);
			if ((message = br.readLine()) != null)
			{
				System.out.println("CLIENT: From Server \"" + message + "\"");
			}
		}
		System.out.println(filename + " Sent Successfully to "
				+ server.getRemoteSocketAddress());
		oos.writeObject(null);
	}

	public void receivefile() throws IOException, ClassNotFoundException
	{
		while ((testheader = (header) ois.readObject()) != null)
		{
			bytesRead = testheader.payload.length;
			ack = testheader.num;
			bout.write(testheader.payload, 0, bytesRead);
			bw.write("ACK#" + ack + "\n");
			bw.flush();
		}
		bout.flush();
		server.close();

		System.out.println("\"received "+filename+"\" saved successfully!");
		bout.close();

	}

	public static void main(String args[]) throws IOException, ClassNotFoundException
	{
		System.out.println("SERVER: Enter Port #:");
		port = Integer.parseInt(scanner.nextLine());
		MyServer testserver = new MyServer(port);

		while (true)
		{
			testserver.messageexchange();
			System.out.println("SERVER: back in main");
			if (command != null)
			{
				commandarr = command.split(" ", 2);
				filename = commandarr[1];

				if ((commandarr[0]).equals("receive"))
				{
					System.out.println("SERVER: Going to receive file.");
					fout = new FileOutputStream("received " + filename);
					bout = new BufferedOutputStream(fout);
					ois = new ObjectInputStream(is);
					testserver.receivefile();
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

					fin = new FileInputStream(file);
					bin = new BufferedInputStream(fin);
					oos = new ObjectOutputStream(os);
					System.out.println("SERVER: Got command to " + command);
					testserver.sendfile(file);
					os.flush();
					bin.close();
					server.close();
					break;
				}
			}
			testserver.close();
		}

	}
}
