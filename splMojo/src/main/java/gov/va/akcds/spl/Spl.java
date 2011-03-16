package gov.va.akcds.spl;

import java.io.File;

public class Spl {

	//
	// instance variables
	//

	private File xmlFile;
	
	private String splSetId;

	private String splId;

	private String xmlFileName;

	private String xmlFileDir;

	private String xmlFileDateCode;

	private String version;

	private String firstApprovedDate;
	
	//
	// constructor(s)
	//

	public Spl() {
		// public empty constructor is allowed
	}

	public Spl(File file) throws Exception {
		SplFactory.buildSpl(file, this);
	}

	//
	// trivial getters and setters
	//

	public String getSplSetId() {
		return splSetId;
	}

	public void setSplSetId(String splSetId) {
		this.splSetId = splSetId;
	}

	public String getSplId() {
		return splId;
	}

	public void setSplId(String splId) {
		this.splId = splId;
	}

	public String getXmlFileName() {
		return xmlFileName;
	}

	public void setXmlFileName(String xmlFileName) {
		this.xmlFileName = xmlFileName;
	}

	public String getXmlFileDir() {
		return xmlFileDir;
	}

	public void setXmlFileDir(String xmlFileDir) {
		this.xmlFileDir = xmlFileDir;
	}

	public String getXmlFileDateCode() {
		return xmlFileDateCode;
	}

	public void setXmlFileDateCode(String xmlFileDateCode) {
		this.xmlFileDateCode = xmlFileDateCode;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getFirstApprovedDate() {
		return firstApprovedDate;
	}

	public void setFirstApprovedDate(String firstApprovedDate) {
		this.firstApprovedDate = firstApprovedDate;
	}

	public File getXmlFile() {
		return xmlFile;
	}

	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}

}
