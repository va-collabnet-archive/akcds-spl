package gov.va.akcds.util.zipUtil;

/**
 *	Class used to return the data output by the ZipContentsIterator. 
 */

public class ZipFileContent
{
	private String name_;
	private byte[] fileBytes_;
	
	public ZipFileContent(String name, byte[] fileBytes)
	{
		this.name_ = name;
		this.fileBytes_ = fileBytes;
	}

	public String getName()
	{
		return name_;
	}

	public byte[] getFileBytes()
	{
		return fileBytes_;
	}
}
