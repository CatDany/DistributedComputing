package catdany.bfdist.server.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class Zipper
{
	/**
	 * Compress specified files and create a zip-file
	 * @param zipFile Output location for a zip-file
	 * @param toZip Files to compress
	 */
	public static void zip(File zipFile, File[] toZip) throws IOException
	{
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
		for (File i : toZip)
		{
			zipOut.putNextEntry(new ZipEntry(i.getName()));
			FileInputStream fis = new FileInputStream(i);
			IOUtils.copy(fis, zipOut);
			fis.close();
			zipOut.closeEntry();
		}
		zipOut.close();
	}
}