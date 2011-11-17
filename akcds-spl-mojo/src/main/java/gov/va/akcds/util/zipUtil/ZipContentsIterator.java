package gov.va.akcds.util.zipUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Convenience class to read the file contents out of a zip file as an enumeration.
 * @author Dan Armbrust
 */

public class ZipContentsIterator implements Enumeration<ZipFileContent>
{
	private ZipInputStream zis_ = null;
	private ZipFileContent currentEntry_ = null;

	public ZipContentsIterator(InputStream is) throws IOException
	{
		zis_ = new ZipInputStream(is);
		readEntry();
	}
	
	public ZipContentsIterator(byte[] zipFile) throws IOException
	{
		zis_ = new ZipInputStream(new ByteArrayInputStream(zipFile));
		readEntry();
	}

	public ZipContentsIterator(File zipFile) throws IOException
	{
		zis_ = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
		readEntry();
	}

	private void readEntry() throws IOException
	{
		try
		{
			if (zis_ != null)
			{
				ZipEntry inputEntry = zis_.getNextEntry();
				if (inputEntry != null)
				{
					// Read the contents of this entry into a byte array.
					long size = inputEntry.getSize();
					if (size <= 0)
					{
						// size is unknown? This is legal in the spec, don't know why. We only use this as a hint anyway.
						size = 10;
					}

					if (size > Integer.MAX_VALUE)
					{
						throw new IOException("Zip file contains a file too large for this implementation.");
					}
					else
					{
						ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
						byte[] buffer = new byte[10000];

						int readBytes = 0;
						while ((readBytes = zis_.read(buffer)) != -1)
						{
							baos.write(buffer, 0, readBytes);
						}
						currentEntry_ = new ZipFileContent(inputEntry.getName(), baos.toByteArray());
					}
				}
				else
				{
					close();
				}
			}
		}
		catch (IOException e)
		{
			close();
			throw e;
		}
	}

	public boolean hasMoreElements()
	{
		if (currentEntry_ == null)
		{
			try
			{
				readEntry();
			}
			catch (IOException e)
			{
				throw new NoSuchElementException("Unexpected error reading zip file - " + e);
			}
		}
		return currentEntry_ != null;
	}

	public ZipFileContent nextElement()
	{
		if (currentEntry_ == null)
		{
			try
			{
				readEntry();
			}
			catch (IOException e)
			{
				throw new NoSuchElementException("Unexpected error reading zip file - " + e);
			}
		}

		if (currentEntry_ == null)
		{
			throw new NoSuchElementException();
		}
		else
		{
			ZipFileContent temp = currentEntry_;
			currentEntry_ = null;
			return temp;
		}
	}

	/**
	 * close the resources opened with this class. Not necessary if elements were read until hasNextElement returns false (resources are
	 * automatically closed in that case)
	 */
	public void close()
	{
		if (zis_ != null)
		{
			try
			{
				zis_.close();
			}
			catch (IOException e)
			{
				// noop
			}
			zis_ = null;
			currentEntry_ = null;
		}
	}
}
