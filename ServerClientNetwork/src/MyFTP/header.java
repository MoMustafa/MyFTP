package MyFTP;

import java.io.Serializable;

public class header implements Serializable
{
	byte[] payload = null;
	long num = 0;
	
	public header(byte[] data, long sequence)
	{
		payload = data;
		num = sequence;
	}
}
