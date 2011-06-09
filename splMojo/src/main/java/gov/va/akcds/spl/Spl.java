package gov.va.akcds.spl;

import gov.va.akcds.util.zipUtil.ZipContentsIterator;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class Spl
{

	private String zipFileName_;
	private ZipFileContent xmlFile_;
	private ArrayList<ZipFileContent> supportingFiles_;

	private String setId_, version_, firstApprovedDate_;
	private HashSet<NDA> uniqueNdas_ = new HashSet<NDA>();
	
	private HashSet<String> ndaTypesToDrop_ = new HashSet<String>(Arrays.asList(new String[] {"ANADA", "NADA", "part"}));

	
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
		ArrayList<String> rawNDAs = findAllValues(rootElement, Arrays.asList(new String[] {"subjectOf", "approval", "id"}), "extension", 0);
		for (String s : rawNDAs)
		{
			NDA nda = new NDA(s);
			if (!ndaTypesToDrop_.contains(nda.getType())) 
			{
				uniqueNdas_.add(new NDA(s));
			}
		}
		firstApprovedDate_ = getValue(rootElement, new String[] { "verifier", "time" }, "value");
	}
	
	/**
	 * Find all instances of the specified hierarchy under any parent path.
	 * 
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<String> findAllValues(Element lookUnder, List<String> childPath, String attributeName, int childPathPos)
	{
		ArrayList<String> result = new ArrayList<String>();

		for (Element e : (List<Element>) lookUnder.getChildren())
		{
			int localChildpathPos = childPathPos;
			if (!e.getName().equals(childPath.get(localChildpathPos)))
			{
				//each level of the child path must match as we recurse, otherwise, we start over looking for a match.
				localChildpathPos = 0;
			}
			
			if (e.getName().equals(childPath.get(localChildpathPos)))
			{
				if (childPath.size() > localChildpathPos + 1)
				{
					//Check for a match on the next level of the child path.
					result.addAll(findAllValues(e, childPath, attributeName, localChildpathPos + 1));
				}
				else
				{
					//Matched all the way to the the end of the child path.  Store the value.
					String temp;
					if (attributeName == null)
					{
						temp = e.getText();
					}
					else
					{
						temp = e.getAttributeValue(attributeName);
					}
					if (temp != null)
					{
						result.add(temp);
					}
				}
			}

			// recurse down
			result.addAll(findAllValues(e, childPath, attributeName, localChildpathPos));

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
	
	public HashSet<NDA> getUniqueNDAs()
	{
		return uniqueNdas_;
	}
	
	public boolean hasAtLeastOneNDA()
	{
		if (uniqueNdas_ != null && uniqueNdas_.size() > 0)
		{
			return true;
		}
		return false;
	}
}
