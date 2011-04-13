package gov.va.akcds.spl;

import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;

import com.apelon.air.xml.w3cDom.util.XmlUtil;

public class Spl {

	//
	// instance variables
	//

	private ZipFileContent xmlFile_;
	
	private ArrayList<ZipFileContent> supportingFiles_;
	
	private String splSetId;

	private String splId;

	private String zipFileName_;

	private String version;

	private String firstApprovedDate;
	
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
	
	private void init() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Node docNode = db.parse(new ByteArrayInputStream(xmlFile_.getFileBytes())).getDocumentElement();
		
		// spl set id
		Node setIdNode = XmlUtil.getChild(docNode, "setId");
		if (setIdNode != null) {
			splSetId = XmlUtil.getValue(setIdNode, "root");
		}
		// spl id
		Node idNode = XmlUtil.getChild(docNode, "id");
		if (idNode != null) {
			String id = XmlUtil.getValue(idNode, "root");
			splId = id;
		}
		// version
		Node versionNode = XmlUtil.getChild(docNode, "versionNumber");
		if (versionNode != null) {
			version = XmlUtil.getValue(versionNode, "value");
		}
		// first approved date
		Node verifierNode = XmlUtil.getChild(docNode, "verifier");
		if (verifierNode != null) {
			Node timeNode = XmlUtil.getChild(verifierNode, "time");
			if (timeNode != null) {
				firstApprovedDate = XmlUtil.getValue(timeNode, "value");
			}
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

	public String getSplSetId() {
		return splSetId;
	}

	public String getSplId() {
		return splId;
	}

	public String getXmlFileName() {
		return xmlFile_.getName();
	}

	public String getZipFileName() {
		return zipFileName_;
	}

	public String getZipFileDateCode() {
		return zipFileName_.substring(0, 8);
	}

	public String getVersion() {
		return version;
	}

	public String getFirstApprovedDate() {
		return firstApprovedDate;
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
}
