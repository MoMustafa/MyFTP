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
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TCP
{
	private static final int packetsize = 1000;
	private static final long timeout = 500000000;
	private static long starttime = 0;

	/*
	 * This method first creates a buffer. Then it generates a checksum for the
	 * file being sent. It also generates a random long int to be the starting
	 * sequence. The randomly generated long int cannot be zero.
	 * 
	 * Then it checks the length of the file and calculates the last sequence
	 * number.
	 * 
	 * Then it initiates the timer.
	 * 
	 * While the last sequence number is not reached the method keeps sending
	 * packets to the receiver. Each packet has a byte array of a fixed maximum
	 * size, a sequence number and the file's checksum.
	 * 
	 * When we reach the last packet, the number of bytes left may be less than
	 * the packet size. So the byte array's size is adjusted accordingly.
	 * 
	 * Before being sent, the packet is added to the buffer. Then the packet is
	 * sent and the method checks if timeout has occurred. Timeout is determined
	 * by if the time elapsed since the starttime exceeds a certain
	 * predetermined amount.
	 * 
	 * If timeout occurs, then the timeout method is called to check if a
	 * retransmission is necessary.
	 * 
	 * Once all the packets have been sent, we send a null to indicate end of
	 * transmission and the method waits for a response from the recipient to
	 * see if the checksum of received file matches the checksum of the sent
	 * file.
	 */
	public static void send(String filename, Socket socket)
			throws IOException, NoSuchAlgorithmException
	{
		File file = new File(filename);
		OutputStream os = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		FileInputStream fin = new FileInputStream(file);
		BufferedInputStream bin = new BufferedInputStream(fin);

		ArrayList<header> buffer = new ArrayList<header>();
		Random rand = new Random();
		new ChecksumGen();

		String message = null;
		String checksum = ChecksumGen.getChecksum(filename);
		int size = 0;
		long filelen = file.length();
		long sequence = (long) rand.nextInt(Integer.SIZE - 1) + 1;
		long endsequence = sequence + filelen;
		starttime = System.nanoTime();

		System.out.println("Sending " + filename);

		while (sequence != endsequence)
		{
			size = packetsize;
			if (endsequence - sequence >= size)
				sequence += size;
			else
			{
				size = (int) (endsequence - sequence);
				sequence = endsequence;
			}
			byte[] packet = new byte[size];
			bin.read(packet, 0, size);
			header testheader = new header(packet, sequence, checksum);

			buffer.add(testheader);

			System.out.println("Sending packet #" + testheader.num);
			oos.writeObject(testheader);
			if ((message = br.readLine()) != null)
			{
				System.out.println("\t\t\t\t\t\"" + message + "\"");
				if ((System.nanoTime() - starttime) > timeout)
				{
					timeout(message, testheader.num, buffer, oos, socket);
				}
			}
		}
		System.out.println("\"" + filename + "\" sent successfully to "
				+ socket.getRemoteSocketAddress());
		oos.writeObject(null);
		if ((message = br.readLine()) != null)
			System.out.println("Recepient says: \"" + message + "\"");
		bin.close();
		buffer.clear();
	}

	/*
	 * This method first creates a buffer. Then, as long as the sender is
	 * non-null objects we keep reading. At each received packet, we add it to
	 * the buffer and sort the buffer by sequence number.
	 * 
	 * Then we calculate the ack to be 1 packetsize greater than the sequence
	 * number of the packet we just received.
	 * 
	 * To test that our retransmission works, we deliberately drop the 3rd
	 * packet that we received.
	 * 
	 * If the packet just received has a sequence number not equal to the ack we
	 * just sent, we do not update the ack any further and keep sending acks of
	 * the missing packet. Once we receive the retransmission, we update our ack
	 * to be 1 packet size greater than whatever the largest sequence number in
	 * our buffer is.
	 * 
	 * For each iteration, we keep track of the packet's checksum as well.
	 * 
	 * After all packets have been received, we empty the buffer sequentially
	 * and write into our file in that order.
	 * 
	 * After writing the new file, we call a method to generate that file's
	 * checksum and check if the checksum matches that which was sent in the
	 * packets. Then we reply to the sender with our result on the checksum
	 * test.
	 */
	public static void receive(String filename, Socket socket)
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("In RECEIVE TCP");
		FileOutputStream fout = new FileOutputStream("receivedTCP " + filename);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		InputStream is = socket.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		OutputStream os = socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		BufferedWriter bw = new BufferedWriter(osw);

		header testheader = null;
		ArrayList<header> buffer = new ArrayList<header>();
		String testchecksum = null;

		int drop = 0;
		long ack = 0;
		while ((testheader = (header) ois.readObject()) != null)
		{
			drop++;

			if (drop != 3)
			{
				System.out.println("Received packet #" + testheader.num);
				buffer.add(testheader);
				Collections.sort(buffer, header.compareheader);

				if (testheader.num == ack || ack == 0)
				{
					ack = buffer.get(buffer.size() - 1).num + packetsize;
				}
				System.out.println("\t\t\t\t\t Sending ACK #" + ack);
			}
			bw.write("ACK " + ack + "\n");
			bw.flush();

			testchecksum = testheader.checksum;
		}
		for (header tempheader : buffer)
			bout.write(tempheader.payload, 0, tempheader.payload.length);
		bout.close();
		buffer.clear();

		new ChecksumGen();
		String checksum = ChecksumGen.getChecksum("receivedTCP " + filename);
		System.out.println("Testing Checksum");
		if (checksum.equals(testchecksum))
		{
			System.out.println("Checksum matches.");
			bw.write("Checksum matches.");
			bw.flush();
		}
		else
		{
			System.out.println("Checksum doesn't match.");
			bw.write("Checksum doesn't match.");
			bw.flush();
		}
		socket.close();
		System.out.println(
				"\"received " + filename + "\" saved successfully from "
						+ socket.getRemoteSocketAddress());
		bout.close();
	}

	/*
	 * At every timeout, the method first resets the timer. Then it reads the
	 * latest ack that was received. if the ack received is less than the
	 * sequence number of the last packet that was sent, it searches the buffer
	 * to find the packet with a sequence number matching the ack.
	 * 
	 * Once found, the method retransmits that missing packet and waits for a
	 * reply from the recipient before exiting back to the method that called
	 * it.
	 */
	public static void timeout(String message, long seq,
			ArrayList<header> buffer, ObjectOutputStream oos, Socket socket)
			throws IOException
	{
		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		System.out.println("\t\t\tTIMEOUT\n");
		starttime = System.nanoTime();

		String[] temp = message.split(" ", 2);
		long ack = Long.parseLong(temp[1]);
		if ((ack - seq) < 0)
		{
			for (header t : buffer)
			{
				if (t.num == ack)
				{
					System.out.println("Retransmitting packet #" + t.num);
					oos.writeObject(t);
					if ((message = br.readLine()) != null)
					{
						System.out.println("\t\t\t\t\t\"" + message + "\"");
					}
				}
			}
		}
	}
}
