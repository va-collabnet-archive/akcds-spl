package gov.va.akcds.spl;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Node;

import com.apelon.air.xml.w3cDom.util.XmlUtil;

public class SplFactory {

	//
	// instance variables
	//

	private File file;

	private Node document;

	private Spl spl;

	//
	// static methods to generate an spl object for a given file
	//

	public static Spl buildSpl(File file) throws Exception {
		Spl spl = new Spl();
		new SplFactory(file, spl).buildSpl();
		return spl;
	}

	public static Spl buildSpl(File file, Spl spl) throws Exception {
		new SplFactory(file, spl).buildSpl();
		spl.setXmlFile(file);
		return spl;
	}

	//
	// static method to get the xml file for a given root directory
	//
	
	public static Spl getSplFromRootDir(File rootDir) throws Exception {
		File[] files = rootDir.listFiles();
		Spl spl = null;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.getCanonicalPath().toLowerCase().endsWith((".xml"))) {
				System.out.println("FILE NAME: " + file.getName());
				spl = new Spl(file);
				return spl;
			}
		}
		return spl;
	}
	
	//
	// constructor
	//

	public SplFactory(File file, Spl spl) throws Exception {
		this.file = file;
		this.spl = spl;
		this.document = XmlUtil.getNode(this.file, "document");
	}

	//
	// buildSpl()
	//

	public void buildSpl() throws Exception {
		initIds();
		initFileInfo();
	}

	//
	// initIds()
	//

	private void initIds() throws Exception {
		// spl set id
		Node setIdNode = XmlUtil.getChild(document, "setId");
		if (setIdNode != null) {
			String splSetId = XmlUtil.getValue(setIdNode, "root");
			spl.setSplSetId(splSetId);
		}
		// spl id
		Node idNode = XmlUtil.getChild(document, "id");
		if (idNode != null) {
			String id = XmlUtil.getValue(idNode, "root");
			spl.setSplId(id);
		}
		// version
		Node versionNode = XmlUtil.getChild(document, "versionNumber");
		if (versionNode != null) {
			spl.setVersion(XmlUtil.getValue(versionNode, "value"));
		}
		// first approved date
		Node verifierNode = XmlUtil.getChild(document, "verifier");
		if (verifierNode != null) {
			Node timeNode = XmlUtil.getChild(verifierNode, "time");
			if (timeNode != null) {
				String time = XmlUtil.getValue(timeNode, "value");
				spl.setFirstApprovedDate(time);
			}
		}
	}

	private void initFileInfo() throws Exception {
		String fileName = this.file.getName();
		this.spl.setXmlFileName(fileName);
		String dirName = this.file.getParentFile().getName();
		this.spl.setXmlFileDir(dirName);
		String docDateCode = dirName.substring(0, 8);
		this.spl.setXmlFileDateCode(docDateCode);
	}

}
