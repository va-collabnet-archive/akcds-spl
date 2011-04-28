package gov.va.akcds.spl;

import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class Spl
{

	private String zipFileName_;
	private ZipFileContent xmlFile_;
	private ArrayList<ZipFileContent> supportingFiles_;

	private String setId_, version_, firstApprovedDate_;
	private HashSet<String> uniqueNdas_ = new HashSet<String>();

	public Spl(ArrayList<ZipFileContent> files, String sourceZipFileName) throws Exception
	{
		supportingFiles_ = new ArrayList<ZipFileContent>();
		zipFileName_ = sourceZipFileName;
		for (ZipFileContent zfc : files)
		{
			if (zfc.getName().toLowerCase().endsWith("xml"))
			{
				xmlFile_ = zfc;
			}
			else
			{
				supportingFiles_.add(zfc);
			}
		}
		init();

	}

	private void init() throws Exception
	{

		SAXBuilder sb = new SAXBuilder();
		Document xmlDoc = sb.build(new ByteArrayInputStream(xmlFile_.getFileBytes()));

		Element rootElement = xmlDoc.getRootElement();

		setId_ = getValue(rootElement, "setId", "root");
		version_ = getValue(rootElement, "versionNumber", "value");
		uniqueNdas_.addAll(findAllValues(rootElement, Arrays.asList(new String[] {"subjectOf", "approval", "id"}), "extension"));
		
		firstApprovedDate_ = getValue(rootElement, new String[] { "verifier", "time" }, "value");
	}
	
	/**
	 * Find all instances of the specified hierarchy under any parent path.
	 * 
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<String> findAllValues(Element lookUnder, List<String> childPath, String attributeName)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (Element e : (List<Element>)lookUnder.getChildren())
		{
			if (e.getName().equals(childPath.get(0)))
			{
				if (childPath.size() > 1)
				{
					result.addAll(findAllValues(e, childPath.subList(1, childPath.size()), attributeName));
				}
				else
				{
					String temp = e.getAttributeValue(attributeName);
					if (temp != null)
					{
						result.add(temp);
					}
				}
			}

			//recurse down
			result.addAll(findAllValues(e, childPath, attributeName));

		}
		return result;
	}

	private String getValue(Element lookUnder, String[] childPath, String attributeName)
	{
		Element current = lookUnder;
		for (String child : childPath)
		{
			current = current.getChild(child, current.getNamespace());
			if (current == null)
			{
				return null;
			}
		}
		return current.getAttributeValue(attributeName);
	}
	
	private String getValue(Element lookUnder, String childName, String attributeName)
	{
		try
		{
			return lookUnder.getChild(childName, lookUnder.getNamespace()).getAttributeValue(attributeName);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	//
	// trivial getters and setters
	//

	@Override
	public String toString()
	{
		return "SPL from Zip " + zipFileName_;
	}

	public String getSetId()
	{
		return setId_;
	}

	public String getXmlFileName()
	{
		return xmlFile_.getName();
	}

	public String getZipFileName()
	{
		return zipFileName_;
	}

	public String getZipFileDateCode()
	{
		return zipFileName_.substring(0, 8);
	}

	public String getVersion()
	{
		return version_;
	}

	public String getFirstApprovedDate()
	{
		return firstApprovedDate_;
	}

	public ArrayList<ZipFileContent> getSupportingFiles()
	{
		return supportingFiles_;
	}

	public String getXMLFileAsString()
	{
		return new String(xmlFile_.getFileBytes());
	}

	public int getXMLFileLength()
	{
		return xmlFile_.getFileBytes().length;
	}
	
	public HashSet<String> getUniqueNDAs()
	{
		return uniqueNdas_;
	}
}
