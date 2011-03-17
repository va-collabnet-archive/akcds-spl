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
import gov.va.akcds.wbSplFileExtractor.SplFileExtractor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;

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

	// constant uuids

	private UUID preferredTermUuid = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
			.getPrimoridalUid();

	// uuids populated when the concepts for them are created

	private UUID ndaUuid;

	private UUID splXmlTextUuid;

	private UUID splSetIdUuid;

	private UUID splRootUuid;

	// EConceptFactory

	private EConceptFactory eConceptFactory = new EConceptFactory(SEED);

	public SplMojo() throws Exception {
	}

	public void execute() throws MojoExecutionException {

		try {

			// output directory
			File file = outputDirectory;
			if (file.exists() == false) {
				file.mkdirs();
			}

			// jbin (output) file
			File jbinFile = new File(file, "ExampleConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(jbinFile)));

			// write the rel concepts
			this.ndaUuid = writeTerminologyAuxConcept("NDA", dos).primordialUuid;
			this.splSetIdUuid = writeTerminologyAuxConcept("SPL_SET_ID", dos).primordialUuid;
			this.splXmlTextUuid = writeTerminologyAuxConcept("SPL XML Text",
					dos).primordialUuid;
			this.splRootUuid = writeSplRootConcept(dos).primordialUuid;

			// source file (splSrcData.zip is a zip of zips)
			File dataDir = new File(file.getParentFile(), "data");
			String dataFileName = "splSrcData.zip";

			// extract the zip of zips
			System.out.println("Extracting zip file...");
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
				String msg = "";
				msg += i + " of " + splFiles.length + ": ";
				msg += "Writing eConcept for: " + rootDir;
				System.out.println(msg);
				Spl spl = SplFactory.getSplFromRootDir(rootDir);
				writeEConcepts(dos, spl);
			}

			dos.flush();
			dos.close();

		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private EConcept writeSplRootConcept(DataOutputStream dos) throws Exception {
		EConcept concept = this.eConceptFactory.writeNamedEConcept("SPL", dos);
		return concept;
	}

	private void writeEConcepts(DataOutputStream dos, Spl spl) throws Exception {

		// echo status
		System.out.println("Writing SPL:");
		System.out.println("SET_ID: " + spl.getSplSetId());
		System.out.println("SPL_ID: " + spl.getSplId());

		// get the uuids
		UUID primordial = UUID.nameUUIDFromBytes((SEED + "." + spl.getSplId())
				.getBytes());

		// create the concept
		EConcept concept = this.eConceptFactory.newInstance(primordial);

		// add an annotation to the conceptAttributes
		String annotationString = "<xml>this is where the xml should go</xml>";
		this.eConceptFactory.addAnnotation(concept.conceptAttributes,
				this.splXmlTextUuid, annotationString);

		this.eConceptFactory.addDescription(concept, this.splSetIdUuid, spl
				.getSplId());
		this.eConceptFactory.addDescription(concept, this.preferredTermUuid,
				spl.getSplId());
		this.eConceptFactory.addDescription(concept, this.splXmlTextUuid,
				"<xml>This is some xml</xml>");
		this.eConceptFactory.addDescription(concept, this.ndaUuid,
				"this is an NDA");

		// create the concept relationships

		this.eConceptFactory.addRelationship(concept, splRootUuid);

		System.out.println("ERROR OCCURS HERE:");
		concept.writeExternal(dos);
		System.out.println("...and we never get to here");
		dos.flush();

	}

	private EConcept writeTerminologyAuxConcept(String name,
			DataOutputStream dos) throws Exception {
		EConcept concept = this.eConceptFactory
				.createTerminologyAuxConcept(name);
		concept.writeExternal(dos);
		dos.flush();
		return concept;
	}

}
