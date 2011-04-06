package gov.va.akcds.splMojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import gov.va.akcds.spl.Spl;
import gov.va.akcds.spl.SplFactory;
import gov.va.akcds.util.eConceptFactory.EConceptFactory;
import gov.va.akcds.util.time.Time;
import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;
import gov.va.akcds.wbSplFileExtractor.SplFileExtractor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.etypes.EConcept;

import com.apelon.air.fileUtil.FileUtil;

/**
 * Goal to generate spl data file
 * 
 * @goal generate-example-data
 * 
 * @phase process-sources
 */

public class SplMojo extends AbstractMojo {

	//
	// class variables
	//

	public static final String SEED = "gov.va.spl";

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;

	// uuids populated when the concepts for them are created

	private UUID splXmlTextUuid;

	private UUID splSetIdUuid;

	private UUID drugNameUuid;

	private UUID splRootUuid;

	private UUID draftFactUuid;

	private UUID splTerminologyAuxConceptRootUuid;

	// other instance variables

	private int xmlFileCnt = 0;

	private int xmlFileTooBigCnt = 0;

	// EConceptFactory

	private EConceptFactory eConceptFactory = new EConceptFactory(SEED);

	// DraftFacts

	private DraftFacts draftFacts;

	//
	// constructor
	//

	public SplMojo() throws Exception {
	}

	/**
	 * 
	 * Method used by maven to create the .jbin data file.
	 * 
	 */

	public void execute() throws MojoExecutionException {

		try {

			// echo status
			System.out.println("TPS report completed 04/05/2011.");
			System.out.println("Starting creation of .jbin file for Structured Product Labels (SPLs)");
			System.out.println(Time.now());
			
			// output directory
			File file = outputDirectory;
			if (file.exists() == false) {
				file.mkdirs();
			}

			// jbin (output) file
			File jbinFile = new File(file, "ExampleConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(jbinFile)));

			// create the root concept for the spl terminology types
			this.splTerminologyAuxConceptRootUuid = this.eConceptFactory
					.writeTerminologyAuxPath("VA AKCDS/Types", dos).primordialUuid;

			// create the terminology aux concepts (types, essentially)
			this.splSetIdUuid = this.eConceptFactory.writeNamedConcept(
					this.splTerminologyAuxConceptRootUuid, "SPL_SET_ID", dos).primordialUuid;
			this.draftFactUuid = this.eConceptFactory.writeNamedConcept(
					this.splTerminologyAuxConceptRootUuid, "Draft Fact", dos).primordialUuid;
			this.drugNameUuid = this.eConceptFactory.writeNamedConcept(
					this.splTerminologyAuxConceptRootUuid, "Drug Name", dos).primordialUuid;
			this.splXmlTextUuid = this.eConceptFactory.writeNamedConcept(
					this.splTerminologyAuxConceptRootUuid, "SPL XML Text", dos).primordialUuid;

			// create the spl root concept (the concept named "SPL")
			this.splRootUuid = writeSplRootConcept(dos).primordialUuid;

			// get the data directory
			File dataDir = new File(file.getParentFile(), "data");

			// load the draft facts
			System.out.println("Loading draft facts:");
			File draftFactsFile = new File(dataDir, "splDraftFacts.txt");
			this.draftFacts = new DraftFacts();
			this.draftFacts.init(draftFactsFile);
			System.out.println("\nDone loading draft facts.");

			// source file (splSrcData.zip is a zip of zips)
			String dataFileName = "splSrcData.zip";

			// extract the zip of zips
			System.out.println("Extracting zip of zips.");
			System.out.println("     dataDir: " + dataDir);
			System.out.println("dataFileName: " + dataFileName);
			File xmlRoot = SplFileExtractor.extractSplFiles(dataDir,
					dataFileName);

			// write the eConcepts for each spl
			File[] splFiles = xmlRoot.listFiles();
			for (int i = 0; i < splFiles.length; i++) {
				File rootDir = splFiles[i];
				if (rootDir.getName().toLowerCase().endsWith(".svn")) {
					// skip .svn directories
					continue;
				}
				System.out.print(".");
				if(i % 50 == 0) {
					System.out.println("");
				}
				Spl spl = SplFactory.getSplFromRootDir(rootDir);
				writeEConcept(dos, spl);
			}

			// write the meta data concepts
			System.out.println("TOTAL SPL FILES:   " + xmlFileCnt);
			System.out.println("FILE TO BIG FILES: " + xmlFileTooBigCnt);

			dos.flush();
			dos.close();

		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private EConcept writeTerminologyAuxConcept(String name,
			DataOutputStream dos) throws Exception {
		return this.eConceptFactory.writeTerminologyAuxConcept(name, dos);
	}

	private EConcept writeSplRootConcept(DataOutputStream dos) throws Exception {
		EConcept concept = this.eConceptFactory.writeNamedConcept("SPL", dos);
		return concept;
	}

	private void writeEConcept(DataOutputStream dos, Spl spl) throws Exception {

		// get the facts
		ArrayList<DraftFact> splDraftFacts = draftFacts.getFacts(spl
				.getSplSetId());

		// if there are no facts don't add the spl (it complicates things)
		if (splDraftFacts.size() > 0) {

			// get the primordial uuid
			UUID primordial = UUID.nameUUIDFromBytes((SEED + "." + spl
					.getSplSetId()).getBytes());

			// create the concept
			EConcept concept = this.eConceptFactory.newInstance(primordial);

			// add the splSetId as an id
			this.eConceptFactory.addIdentifier(concept, this.splSetIdUuid, spl
					.getSplSetId());

			// add the preferred term to the concept
			String drugName = splDraftFacts.get(0).getDrugName();
			String preferredTerm = drugName;

			this.eConceptFactory.addPreferredTerm(concept, preferredTerm);

			// add an annotation to the conceptAttributes for each draft fact
			if (splDraftFacts != null) {
				for (int i = 0; i < splDraftFacts.size(); i++) {
					DraftFact fact = splDraftFacts.get(i);
					String annotationString = "";
					annotationString += fact.getDrugName() + "|";
					annotationString += fact.getDrugCode() + "|";
					annotationString += fact.getRoleName() + "|";
					annotationString += fact.getRoleId() + "|";
					annotationString += fact.getConceptName() + "|";
					annotationString += fact.getConceptCode() + "|";
					this.eConceptFactory.addAnnotation(
							concept.conceptAttributes, this.draftFactUuid,
							annotationString);
				}
			}

			// add drug name as a description to the concept
			this.eConceptFactory.addDescription(concept, this.drugNameUuid,
					drugName);

			// add xml text as a description to the concept
			String xmlTxt = FileUtil.getAsString(spl.getXmlFile());
			int xmlTxtSize = xmlTxt.getBytes().length;
			if (xmlTxtSize > 64000) {
				xmlTxt = "XML SIZE > 64K BYTES. COULD NOT BE IMPORTED.";
				this.xmlFileTooBigCnt++;
			}
			this.xmlFileCnt++;
			this.eConceptFactory.addAnnotation(concept, this.splXmlTextUuid,
					xmlTxt);

			// create the concept relationships
			this.eConceptFactory.addRelationship(concept, splRootUuid);

			// write the concept to the .jbin file
			concept.writeExternal(dos);
			dos.flush();
		}

	}

}
