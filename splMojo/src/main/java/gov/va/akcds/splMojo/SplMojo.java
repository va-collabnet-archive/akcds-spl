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
import gov.va.akcds.util.fileUtil.FileUtil;
import gov.va.akcds.wbSplFileExtractor.SplFileExtractor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.tapi.TerminologyException;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

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

	private static int relCnt;

	private static int annotationCnt = 0;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;

	private UUID preferredDescriptionType = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
			.getPrimoridalUid();

	// constant uuids

	private UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE
			.getPrimoridalUid();

	private UUID author = ArchitectonicAuxiliary.Concept.USER
			.getPrimoridalUid();

	private UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
			.getPrimoridalUid();

	private UUID isA = ArchitectonicAuxiliary.Concept.IS_TERM_OF
			.getPrimoridalUid();

	// uuids populated when the concepts for them are created

	private UUID ndaUuid;

	private UUID splXmlTextUuid;

	private UUID splSetIdUuid;

	private UUID splRootUuid;

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
			this.ndaUuid = writeRelEConcept("NDA", dos);
			this.splSetIdUuid = writeRelEConcept("SPL_SET_ID", dos);
			this.splXmlTextUuid = writeRelEConcept("SPL XML Text", dos);
			this.splRootUuid = writeSplRootConcept(dos);

			// source file (splSrcData.zip is a zip of zips)
			File dataDir = new File(file.getParentFile(), "data");
			String dataFileName = "splSrcData.zip";

			// extract the zip of zips
			System.out.println("Extracting zip file...");
			System.out.println("     dataDir: " + dataDir);
			System.out.println("dataFileName: " + dataFileName);
			File xmlRoot = SplFileExtractor.extractSplFiles(dataDir,
					dataFileName);

			// write the eConcepts for each spl directory
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

	private UUID writeSplRootConcept(DataOutputStream dos) throws Exception {

		// echo status
		System.out.println("Writing SPL ROOT CONCEPT:");

		// get the time
		long time = System.currentTimeMillis();

		// get the uuids
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID primordial = UUID.nameUUIDFromBytes((SEED + ".spl").getBytes());

		// create the concept
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);

		// create the concept attributes
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author);
		conceptAttributes.defined = false;
		conceptAttributes.primordialUuid = primordial;
		conceptAttributes.statusUuid = currentUuid;
		conceptAttributes.setPathUuid(path);
		conceptAttributes.setTime(time);

		// associate the attributes with the concept
		concept.setConceptAttributes(conceptAttributes);

		// create the concept descriptions
		List<TkDescription> descriptions = new ArrayList<TkDescription>();
		TkDescription description = new TkDescription();
		description.setConceptUuid(primordial);
		description.setLang("en");
		description.setPrimordialComponentUuid(UUID.randomUUID());
		description.setPathUuid(path);
		description.setTypeUuid(preferredDescriptionType);
		description.text = "SPL";
		description.setAuthorUuid(author);
		description.setStatusUuid(currentUuid);
		description.setTime(time);
		descriptions.add(description);
		concept.setDescriptions(descriptions);

		// write the concept
		concept.writeExternal(dos);
		dos.flush();

		return primordial;

	}

	private void writeEConcepts(DataOutputStream dos, Spl spl) throws Exception {

		// echo status
		System.out.println("Writing SPL:");
		System.out.println("SET_ID: " + spl.getSplSetId());
		System.out.println("SPL_ID: " + spl.getSplId());

		// get the time
		long time = System.currentTimeMillis();

		// get the uuids
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID primordial = UUID.nameUUIDFromBytes((SEED + "." + spl.getSplId())
				.getBytes());

		// create the concept
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);

		// create the concept attributes
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author);
		conceptAttributes.defined = false;
		conceptAttributes.primordialUuid = primordial;
		conceptAttributes.statusUuid = currentUuid;
		conceptAttributes.setPathUuid(path);
		conceptAttributes.setTime(time);

		// add an annotation to the conceptAttributes
		addAnnotation(conceptAttributes, spl.getXmlFile());

		// associate the attributes with the concept
		concept.setConceptAttributes(conceptAttributes);

		// descriptions
		List<TkDescription> descriptions = new ArrayList<TkDescription>();
		// splSetId (preferredTerm)
		TkDescription splSetId = new TkDescription();
		descriptions.add(splSetId);
		splSetId.setLang("en");
		splSetId.setStatusUuid(currentUuid);
		splSetId.setPrimordialComponentUuid(UUID.randomUUID());
		splSetId.setConceptUuid(primordial);
		splSetId.setPathUuid(path);
		splSetId.setAuthorUuid(author);
		splSetId.setTime(time);
		splSetId.setTypeUuid(preferredTerm);
		splSetId.text = spl.getSplId();
		// splSetId (splSetId)
		TkDescription splPreferredTerm = new TkDescription();
		descriptions.add(splPreferredTerm);
		splPreferredTerm.setLang("en");
		splPreferredTerm.setStatusUuid(currentUuid);
		splPreferredTerm.setPrimordialComponentUuid(UUID.randomUUID());
		splPreferredTerm.setConceptUuid(primordial);
		splPreferredTerm.setPathUuid(path);
		splPreferredTerm.setAuthorUuid(author);
		splPreferredTerm.setTime(time);
		splPreferredTerm.setTypeUuid(this.splSetIdUuid);
		splPreferredTerm.text = spl.getSplId();
		// nda
		TkDescription nda = new TkDescription();
		descriptions.add(nda);
		nda.setLang("en");
		nda.setStatusUuid(currentUuid);
		nda.setPrimordialComponentUuid(UUID.randomUUID());
		nda.setConceptUuid(primordial);
		nda.setPathUuid(path);
		nda.setAuthorUuid(author);
		nda.setTime(time);
		nda.setTypeUuid(this.ndaUuid);
		nda.text = "THIS IS WHERE THE NDA GOES";

		//
		// add the descriptions to the concept
		//

		concept.setDescriptions(descriptions);

		// create the concept relationships

		List<TkRelationship> relationships = new ArrayList<TkRelationship>();
		TkRelationship heirRel = createRelationships(concept, splRootUuid);
		relationships.add(heirRel);
		concept.setRelationships(relationships);

		concept.writeExternal(dos);
		dos.flush();

	}

	private void addAnnotation(TkComponent<?> component, File xmlFile)
			throws Exception {
		
		String xmlTxt = FileUtil.getAsString(xmlFile);
		
		annotationCnt++;
		long time = System.currentTimeMillis();
		List<TkRefsetAbstractMember<?>> annotations = new ArrayList<TkRefsetAbstractMember<?>>();
		TkRefsetStrMember strRefexMember = new TkRefsetStrMember();
		strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
		strRefexMember.setStrValue(xmlTxt);
		
		strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes(((SEED
				+ "." + annotationCnt).getBytes())));

		// strRefexMember.setRefsetUuid(preferredDescriptionType);
		strRefexMember.setRefsetUuid(this.splXmlTextUuid);

		strRefexMember.setStatusUuid(ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid());
		strRefexMember.setAuthorUuid(ArchitectonicAuxiliary.Concept.USER
				.getPrimoridalUid());
		strRefexMember.setPathUuid(ArchitectonicAuxiliary.Concept.SNOMED_CORE
				.getPrimoridalUid());
		strRefexMember.setTime(time);
		annotations.add(strRefexMember);
		component.setAnnotations(annotations);
	}

	private TkRelationship createRelationships(EConcept eConcept,
			UUID targetPrimordial) throws IOException, TerminologyException {

		relCnt++;
		long time = System.currentTimeMillis();

		// uuids
		UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL
				.getPrimoridalUid();
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();

		// create the relationship object
		TkRelationship rel = new TkRelationship();
		rel.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((SEED + relCnt)
				.getBytes()));
		rel.setC1Uuid(eConcept.getPrimordialUuid());
		rel.setTypeUuid(relPrimordial);
		rel.setC2Uuid(targetPrimordial);
		rel
				.setCharacteristicUuid(ArchitectonicAuxiliary.Concept.DEFINING_CHARACTERISTIC
						.getPrimoridalUid());
		rel.setRefinabilityUuid(ArchitectonicAuxiliary.Concept.NOT_REFINABLE
				.getPrimoridalUid());
		rel.setStatusUuid(currentUuid);
		rel.setAuthorUuid(author);
		rel.setPathUuid(path);
		rel.setTime(time);
		rel.setRelGroup(0);

		return rel;

	}

	public UUID writeRelEConcept(String relName, DataOutputStream dos)
			throws Exception {
		long time = System.currentTimeMillis();

		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
				.getPrimoridalUid();
		UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL
				.getPrimoridalUid();
		UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT
				.getPrimoridalUid();

		EConcept concept = new EConcept();
		UUID primordial = UUID.nameUUIDFromBytes((SEED + relName).getBytes());
		concept.setPrimordialUuid(primordial);
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author);

		conceptAttributes = new EConceptAttributes();
		conceptAttributes.defined = false;
		conceptAttributes.primordialUuid = primordial;
		conceptAttributes.statusUuid = currentUuid;
		conceptAttributes.setPathUuid(path);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);

		List<TkDescription> descriptions = new ArrayList<TkDescription>();
		TkDescription description = new TkDescription();
		description.setConceptUuid(primordial);
		description.setLang("en");
		description.setPrimordialComponentUuid(UUID.randomUUID());

		description.setTypeUuid(preferredTerm);
		description.text = relName;
		description.setStatusUuid(currentUuid);
		description.setAuthorUuid(author);
		description.setPathUuid(path);
		description.setTime(time);
		descriptions.add(description);
		concept.setDescriptions(descriptions);

		List<TkRelationship> relationships = new ArrayList<TkRelationship>();
		TkRelationship heirRel = createRelationships(concept, archRoot);
		relationships.add(heirRel);
		concept.setRelationships(relationships);

		System.out.println("Wrote: " + concept);
		concept.writeExternal(dos);

		return primordial;

	}

}
