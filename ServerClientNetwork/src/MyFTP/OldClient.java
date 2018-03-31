package MyFTP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class OldClient extends Socket
{
	private static Socket client = null;
	private static int packetsize = 1000;

	public static void main(String args[])
	{
		OldClient testclient = new OldClient("localhost", 6579);
		testclient.requestfile();

		try
		{
			byte[] contents = new byte[packetsize];
			FileOutputStream fout = new FileOutputStream("received.jpg");
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			InputStream is = client.getInputStream();

			int bytesRead = 0;

			while ((bytesRead = is.read(contents)) != -1)
				bout.write(contents, 0, bytesRead);

			bout.flush();
			client.close();

			System.out.println("File saved successfully!");
			bout.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void requestfile()
	{
		Scanner reader = new Scanner(System.in);
		System.out.println("CLIENT: Enter filename: ");
		String filename = "testimg.jpg";
		System.out.println("CLIENT: Requesting "+filename);
		try
		{
			OutputStream out = client.getOutputStream();
			OutputStreamWriter ow = new OutputStreamWriter(out);
			BufferedWriter bw = new BufferedWriter(ow);
			bw.write(filename);
			bw.flush();

			InputStream ins = client.getInputStream();
			InputStreamReader inr = new InputStreamReader(ins);
			BufferedReader br = new BufferedReader(inr);

			String message = br.readLine();
			System.out.println("CLIENT: SERVER says " + message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		reader.close();
	}

	public OldClient(String serveraddress, int port)
	{
		try
		{
			System.out.println("CLIENT: Connecting to " + serveraddress
					+ " on port " + port);
			client = new Socket(serveraddress, port);
			System.out.println("CLIENT: Just connected to "
					+ client.getRemoteSocketAddress());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
