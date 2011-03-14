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

	public static final String SEED = "gov.va.spl.root";

	private static int annotationCnt = 0;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;

	private int relCnt;

	private UUID rootUuid;

	private UUID preferredDescriptionType;

	public SplMojo() throws Exception {
		this.preferredDescriptionType = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
				.getPrimoridalUid();
	}

	public void execute() throws MojoExecutionException {

		try {

			// output directory
			File file = outputDirectory;
			if (file.exists() == false) {
				file.mkdirs();
			}

			// source file (splSrcData.zip is a zip of zips)
			File dataDir = new File(file.getParentFile(), "data");
			String dataFileName = "splSrcData.zip";

			// extract the zip of zips
			System.out.println("Extracting zip file...");
			System.out.println("     dataDir: " + dataDir);
			System.out.println("dataFileName: " + dataFileName);
			File xmlRoot = SplFileExtractor.extractSplFiles(dataDir,
					dataFileName);

			// jbin (output) file
			File jbinFile = new File(file, "ExampleConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(jbinFile)));

			// write the eConcepts for each spl directory
			writeSplRootConcept(dos);
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
				Spl spl = getSpl(rootDir);
				writeEConcepts(dos, spl);
			}

			dos.flush();
			dos.close();

		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private void writeSplRootConcept(DataOutputStream dos) throws Exception {

		// echo status
		System.out.println("Writing SPL ROOT CONCEPT:");

		// get the time
		long time = System.currentTimeMillis();

		// get the uuids
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE
				.getPrimoridalUid();
		UUID isA = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
		UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
		UUID primordial = UUID.nameUUIDFromBytes(SEED.getBytes());

		this.rootUuid = primordial;

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

	}

	private void writeEConcepts(DataOutputStream dos, Spl spl) throws Exception {

		// echo status
		System.out.println("Writing SPL:");
		System.out.println("  SET_ID: " + spl.getSplSetId());
		System.out.println("  SPL_ID: " + spl.getSplId());

		// get the time
		long time = System.currentTimeMillis();

		// get the uuids
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE
				.getPrimoridalUid();
		UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
				.getPrimoridalUid();
		UUID isA = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
		UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
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
		addAnnotation(conceptAttributes);

		// associate the attributes with the concept
		concept.setConceptAttributes(conceptAttributes);

		// create the concept descriptions (this is where the spl_set_id is
		// added to the concept
		List<TkDescription> descriptions = new ArrayList<TkDescription>();
		TkDescription description = new TkDescription();
		description.setConceptUuid(primordial);
		description.setLang("en");
		description.setPrimordialComponentUuid(UUID.randomUUID());
		description.setPathUuid(path);
		description.setTypeUuid(preferredTerm);
		description.text = spl.getSplId();
		description.setAuthorUuid(author);
		description.setStatusUuid(currentUuid);
		description.setTime(time);
		descriptions.add(description);
		concept.setDescriptions(descriptions);

		// create the concept relationships

		List<TkRelationship> relationships = new ArrayList<TkRelationship>();
		TkRelationship heirRel = createRelationships(concept, rootUuid);
		relationships.add(heirRel);
		concept.setRelationships(relationships);

		concept.writeExternal(dos);
		dos.flush();

	}

	private void addAnnotation(TkComponent<?> component) throws Exception {
		annotationCnt++;
		long time = System.currentTimeMillis();
		List<TkRefsetAbstractMember<?>> annotations = new ArrayList<TkRefsetAbstractMember<?>>();
		TkRefsetStrMember strRefexMember = new TkRefsetStrMember();
		strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
		strRefexMember.setStrValue("THIS IS TEST ANNOTATION " + annotationCnt);

		strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes(((SEED
				+ "." + annotationCnt).getBytes())));

		strRefexMember.setRefsetUuid(preferredDescriptionType);

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

	private Spl getSpl(File rootDir) throws Exception {
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

	private TkRelationship createRelationships(EConcept eConcept,
			UUID targetPrimordial) throws IOException, TerminologyException {
		relCnt++;
		UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL
				.getPrimoridalUid();
		long time = System.currentTimeMillis();
		UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
				.getPrimoridalUid();
		UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE
				.getPrimoridalUid();
		UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();

		TkRelationship rel = new TkRelationship();
		rel.setPrimordialComponentUuid(UUID
				.nameUUIDFromBytes(("gov.va.spl." + relCnt).getBytes()));
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

}
