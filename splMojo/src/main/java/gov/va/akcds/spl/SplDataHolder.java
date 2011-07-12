package gov.va.akcds.spl;

import gov.va.akcds.util.ConsoleUtil;
import gov.va.akcds.util.zipUtil.ZipContentsIterator;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SplDataHolder
{
	Hashtable<String, ArrayList<LocationPointer>> setIdMap_ = new Hashtable<String, ArrayList<LocationPointer>>();
	
	private class LocationPointer
	{
		File containingZipFile_;
		String fullFileNameInsideZipFile_;
		
		public LocationPointer(File containingZipFile, String fullFileNameInsideFile)
		{
			this.containingZipFile_ = containingZipFile;
			this.fullFileNameInsideZipFile_ = fullFileNameInsideFile;
		}
		
		/**
		 * Return just the file name, not the whole nested hierarchy.
		 */
		public String getFileName()
		{
			String fileName = fullFileNameInsideZipFile_;
			// BW is giving us zip files with subdirectories... for some reason. Strip them out.
			if (fileName.lastIndexOf('/') > 0)
			{
				fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());
			}
			if (fileName.lastIndexOf('\\') > 0)
			{
				fileName = fileName.substring(fileName.lastIndexOf('\\') + 1, fileName.length());
			}
			return fileName;
		}
	}

	public SplDataHolder(File[] zipFiles) throws IOException
	{

		for (File f : zipFiles)
		{
			if (!f.getName().toLowerCase().endsWith(".zip"))
			{
				ConsoleUtil.printErrorln("Skipping non-zip file: " + f);
				continue;
			}

			ConsoleUtil.println("Processing " + f);
			ZipContentsIterator outerZCI = new ZipContentsIterator(f);

			while (outerZCI.hasMoreElements())
			{
				// Each of these should be a zip file
				ZipFileContent nestedZipFile = outerZCI.nextElement();

				String fileName = nestedZipFile.getName();
				if (fileName.toLowerCase().endsWith(".zip"))
				{
					LocationPointer lp = new LocationPointer(f, nestedZipFile.getName());
					
					String localFileName = lp.getFileName();

					String setId = localFileName.substring(localFileName.indexOf("_") + 1, localFileName.length() - 4).toUpperCase();
					ArrayList<LocationPointer> pointers = setIdMap_.get(setId);
					if (pointers == null)
					{
						pointers = new ArrayList<LocationPointer>();
						setIdMap_.put(setId, pointers);
					}
					pointers.add(lp);
				}
				else
				{
					ConsoleUtil.printErrorln("Skipping unexpected file in outer zip file: " + nestedZipFile.getName());
				}
			}
			outerZCI.close();
		}
	}

	public Spl getSpl(String setId, String version) throws Exception
	{
		Spl spl = null;
		ArrayList<LocationPointer> splFiles = setIdMap_.get(setId.toUpperCase());

		if ((version != null && version.length() > 0 && !version.equals("-")))
		{
			if (splFiles != null)
			{
				for (LocationPointer lp : splFiles)
				{
					Spl temp = loadSpl(lp);
					if (temp.getVersion().equals(version))
					{
						spl = temp;
						break;
					}
				}
			}
		}
		else
		{
			LocationPointer bestFile = null;

			if (splFiles.size() == 1)
			{
				bestFile = splFiles.get(0);
			}
			else if (version == null || version.length() == 0 || version.equals("-"))
			{
				int newestDate = 0;

				for (LocationPointer lp : splFiles)
				{
					String name = lp.getFileName();
					String date = name.substring(0, name.indexOf('_'));
					int i = Integer.parseInt(date);
					if (i > newestDate)
					{
						bestFile = lp;
					}
				}
			}
			spl = loadSpl(bestFile);
		}
		return spl;
	}

	private Spl loadSpl(LocationPointer lp) throws Exception
	{
		Spl spl = null;
		
		ZipFile zf = new ZipFile(lp.containingZipFile_);
		
		ZipEntry ze = zf.getEntry(lp.fullFileNameInsideZipFile_);
		
		InputStream nestedZipFileStream = zf.getInputStream(ze);
		
		ZipContentsIterator nestedZipFileContents = new ZipContentsIterator(nestedZipFileStream);

		ArrayList<ZipFileContent> filesInNestedZipFile = new ArrayList<ZipFileContent>();

		while (nestedZipFileContents.hasMoreElements())
		{
			filesInNestedZipFile.add(nestedZipFileContents.nextElement());
		}

		if (filesInNestedZipFile.size() > 0)
		{
			// Pass the elements in to the spl factory
			spl = new Spl(filesInNestedZipFile, ze.getName());
		}
		return spl;
	}

}
