package MyFTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumGen
{
	public static String getChecksum(String filename)
			throws NoSuchAlgorithmException, IOException
	{
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		MessageDigest md5Digest = MessageDigest.getInstance("MD5");
		byte[] byteArray = new byte[1024];
		int byteCount = 0;

		while ((byteCount = fis.read(byteArray)) != -1)
		{
			md5Digest.update(byteArray, 0, byteCount);
		}
		fis.close();
		byte[] bytes = md5Digest.digest();
		StringBuilder sb = new StringBuilder();
		
		for(int i=0; i<bytes.length; i++)
		{
			sb.append(Integer.toString((bytes[i]&0xff)+0x100, 16).substring(1));
		}
		
		return sb.toString();
	}
}
