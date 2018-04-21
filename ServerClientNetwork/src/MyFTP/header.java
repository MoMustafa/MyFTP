package MyFTP;

import java.io.Serializable;
import java.util.Comparator;

public class header implements Serializable
{
	private static final long serialVersionUID = 1L;

	byte[] payload = null;
	long num = 0;
	String checksum = null;

	/*
	 * Creates a header object that contains a byte array, a sequence number and
	 * checksum string
	 */
	public header(byte[] data, long sequence, String chksm)
	{
		payload = data;
		num = sequence;
		checksum = chksm;
	}

	/*
	 * Allows the header object to be compares to other header object. This
	 * helps in sorting a data-structure full of header objects. To compare two
	 * header objects the method checks the sequence number of each object and
	 * returns the difference in the sequence numbers. If the the returned
	 * number is negative that means the second object has a larger sequence
	 * number than the first.
	 */
	public static Comparator<header> compareheader = new Comparator<header>()
	{
		public int compare(header h1, header h2)
		{
			int num1 = (int) h1.num;
			int num2 = (int) h2.num;

			return (num1 - num2);
		}
	};
}
