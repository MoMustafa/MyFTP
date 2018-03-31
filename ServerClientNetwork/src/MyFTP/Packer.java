package MyFTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class Packer
{
	public static void createpacket(File f)
	{
		int sequence = 1;
		int size = 500;
		byte buffer[] = new byte[size];
		String filename = f.getName();

		try (FileInputStream fin = new FileInputStream(f);
				BufferedInputStream bin = new BufferedInputStream(fin))
		{
			int bytes = 0;
			while ((bytes = bin.read(buffer)) > 0)
			{
				String packetname = String.format("%s.%03d", filename,
						sequence++);
				File newFile = new File(f.getParent(), packetname);
				try (FileOutputStream fout = new FileOutputStream(newFile))
				{
					fout.write(buffer, 0, bytes);
				}
			}
		} catch (IOException e)
		{
			e.printStackTrace();

		}
	}

	public static List<File> filestomerge(File onefile)
	{
		String tempname = onefile.getName();
		String destfilename = tempname.substring(0,  tempname.lastIndexOf('.'));
		File[] files = onefile.getParentFile().listFiles(
				(File dir, String name)->name.matches(destfilename +"[.]\\d+"));
		Arrays.sort(files);
		return Arrays.asList(files);
	}

	public static void mergepackets(List<File> files, File into)
	{
		try (FileOutputStream fout = new FileOutputStream(into);
				BufferedOutputStream mergingStream = new BufferedOutputStream(
						fout))
		{
			for (File f : files)
			{
				Files.copy(f.toPath(), mergingStream);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
