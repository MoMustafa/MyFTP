package MyFTP;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class OldServer extends Socket
{
	private static ServerSocket listener = null;
	private static Socket server = null;
	private int packetsize = 1000;

	public static void main(String args[])
	{
		OldServer testserver = new OldServer(6579);
		try
		{
			while (true)
			{
				server = listener.accept();
				System.out.println("SERVER: connected to "
						+ server.getRemoteSocketAddress());

				InputStream ins = server.getInputStream();
				InputStreamReader inr = new InputStreamReader(ins);
				BufferedReader br = new BufferedReader(inr);
				String message = br.readLine();
				System.out.println("SERVER : Client is requesting file "
						+ message + "\nChecking if file exists");
				testserver.sendfile(message);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public OldServer(int port)
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
}
