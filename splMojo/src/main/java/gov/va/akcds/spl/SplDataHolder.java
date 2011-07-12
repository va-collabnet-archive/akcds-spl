package gov.va.akcds.spl;

import gov.va.akcds.util.ConsoleUtil;
import gov.va.akcds.util.zipUtil.ZipContentsIterator;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class SplDataHolder
{
	Hashtable<String, ArrayList<File>> setIdMap_ = new Hashtable<String, ArrayList<File>>();

	public SplDataHolder(File[] zipFiles, File expansionFolder) throws IOException
	{
		expansionFolder.mkdirs();

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
					// BW is giving us zip files with subdirectories... for some reason. Strip them out.
					if (fileName.lastIndexOf('/') > 0)
					{
						fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());
					}
					if (fileName.lastIndexOf('\\') > 0)
					{
						fileName = fileName.substring(fileName.lastIndexOf('\\') + 1, fileName.length());
					}

					String setId = fileName.substring(fileName.indexOf("_") + 1, fileName.length() - 4).toUpperCase();
					ArrayList<File> files = setIdMap_.get(setId);
					if (files == null)
					{
						files = new ArrayList<File>();
						setIdMap_.put(setId, files);
					}

					File targetFile = new File(expansionFolder, fileName);
					files.add(targetFile);

					// write out the nested zip to the expansion folder (if it isn't already there)
					if (!targetFile.exists())
					{
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
						bos.write(nestedZipFile.getFileBytes());
						bos.close();
					}
				}
				else
				{
					ConsoleUtil.printErrorln("Skipping unexpected file in outer zip file: " + nestedZipFile.getName());
				}
			}
		}
	}

	public Spl getSpl(String setId, String version) throws Exception
	{
		Spl spl = null;
		ArrayList<File> splFiles = setIdMap_.get(setId.toUpperCase());

		if ((version != null && version.length() > 0 && !version.equals("-")))
		{
			if (splFiles != null)
			{
				for (File f : splFiles)
				{
					Spl temp = loadSpl(f);
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
			File bestFile = null;

			if (splFiles.size() == 1)
			{
				bestFile = splFiles.get(0);
			}
			else if (version == null || version.length() == 0 || version.equals("-"))
			{
				int newestDate = 0;

				for (File f : splFiles)
				{
					String name = f.getName();
					String date = name.substring(0, name.indexOf('_'));
					int i = Integer.parseInt(date);
					if (i > newestDate)
					{
						bestFile = f;
					}
				}
			}
			spl = loadSpl(bestFile);
		}
		return spl;
	}

	private Spl loadSpl(File file) throws Exception
	{
		Spl spl = null;
		// open up the nested zip file
		ZipContentsIterator nestedZipFileContents = new ZipContentsIterator(file);

		ArrayList<ZipFileContent> filesInNestedZipFile = new ArrayList<ZipFileContent>();

		while (nestedZipFileContents.hasMoreElements())
		{
			filesInNestedZipFile.add(nestedZipFileContents.nextElement());
		}

		if (filesInNestedZipFile.size() > 0)
		{
			// Pass the elements in to the spl factory
			spl = new Spl(filesInNestedZipFile, file.getName());
		}
		return spl;
	}

}
