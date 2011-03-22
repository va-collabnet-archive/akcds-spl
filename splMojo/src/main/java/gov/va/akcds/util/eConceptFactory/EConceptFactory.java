package gov.va.akcds.util.eConceptFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.tapi.TerminologyException;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.attribute.TkConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

/**
 * Methods starting in write (e.g. writeConcept) write a concept to the given
 * DataOutputStream. Methods starting in create (e.g. createConcept) creates a
 * concept but does not write that concept to the .jbin file.
 * 
 * @author jgresh
 * 
 */

public class EConceptFactory {

	//
	// instance variables
	//

	private String uuidSeed;

	private static int relCnt = 0;

	private static int annotationCnt = 0;

	// constant uuids

	private UUID pathUuid = ArchitectonicAuxiliary.Concept.SNOMED_CORE
			.getPrimoridalUid();

	private UUID autherUuid = ArchitectonicAuxiliary.Concept.USER
			.getPrimoridalUid();

	private UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT
			.getPrimoridalUid();

	private UUID isAUuid = ArchitectonicAuxiliary.Concept.IS_TERM_OF
			.getPrimoridalUid();

	private UUID preferredTermUuid = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
			.getPrimoridalUid();

	//
	// constructor
	//

	public EConceptFactory(String uuidSeed) throws Exception {
		this.uuidSeed = uuidSeed;
	}

	//
	// methods to create an eConcept
	//

	/**
	 * 
	 * Method to get a new initialized instance of an eConcept
	 * 
	 * @param concept
	 * @param attPrimordialUuid
	 * 
	 */

	public EConcept newInstance(UUID uuid) {
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(uuid);
		this.initAttributes(concept, uuid);
		return concept;
	}

	//
	// method to initialize the attributes of a new concept
	//

	private void initAttributes(EConcept concept, UUID attPrimordialUuid) {
		// create the concept attributes
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		concept.setConceptAttributes(conceptAttributes);
		conceptAttributes.setAuthorUuid(this.autherUuid);
		conceptAttributes.defined = false;
		conceptAttributes.primordialUuid = attPrimordialUuid;
		conceptAttributes.statusUuid = currentUuid;
		conceptAttributes.setPathUuid(this.pathUuid);
		conceptAttributes.setTime(System.currentTimeMillis());
	}

	//
	// create/write a named concept with no parent
	//

	/**
	 * 
	 * Method to create a named concept with no parent/child relationship.
	 * 
	 * @param name
	 * @return
	 * 
	 */

	public EConcept createNamedConcept(String name) {
		// create the concept
		UUID primordial = UUID.nameUUIDFromBytes((this.uuidSeed + "." + name)
				.getBytes());
		EConcept concept = newInstance(primordial);
		addDescription(concept, this.preferredTermUuid, name);
		return concept;
	}

	/**
	 * 
	 * Method to create and write a named concept.
	 * 
	 * @param name
	 * @param dos
	 * @return
	 * @throws Exception
	 */

	public EConcept writeNamedConcept(String name, DataOutputStream dos)
			throws Exception {
		EConcept concept = createNamedConcept(name);
		concept.writeExternal(dos);
		dos.flush();
		return concept;
	}

	//
	// create/write a named concept with a given parent
	//

	/**
	 * 
	 * Method to create a concept as a child of the parent identified by the
	 * given UUID.
	 * 
	 * @param parent
	 * @param name
	 * @return
	 * @throws Exception
	 */

	public EConcept createNamedConcept(UUID parent, String name)
			throws Exception {
		EConcept concept = this.createNamedConcept(name);
		this.addRelationship(concept, parent);
		return concept;
	}

	/**
	 * 
	 * Method to create and write a concept as a child of the parent identified
	 * by the given UUID.
	 * 
	 * @param parent
	 * @param name
	 * @param dos
	 * @return
	 * @throws Exception
	 * 
	 */

	public EConcept writeNamedConcept(UUID parent, String name,
			DataOutputStream dos) throws Exception {
		EConcept concept = this.createNamedConcept(parent, name);
		concept.writeExternal(dos);
		return concept;
	}

	/**
	 * 
	 * Creates the hierarchy defined by the given path starting with the given
	 * UUID as the root of the path. For example, if the UUID for a concept
	 * called "root" is given and "node1/node2/node3" is given a concept called
	 * node1 will be made as a child of "root" and node2 will be made as a child
	 * of node1, etc.
	 * 
	 * @param parent
	 * @param path
	 * @param dos
	 * @return
	 * @throws Exception
	 */

	public EConcept writePath(UUID parent, String path, DataOutputStream dos)
			throws Exception {
		String[] nodes = path.split("/");
		EConcept concept = null;
		for (int i = 0; i < nodes.length; i++) {
			String conceptName = nodes[i];
			concept = createNamedConcept(conceptName);
			this.addRelationship(concept, parent);
			System.out.println("WRITING: " + conceptName);
			concept.writeExternal(dos);
			parent = concept.primordialUuid;
		}
		return concept;
	}

	//
	// attribute
	//

	public void addIdentifier(EConcept concept, UUID authorityUuid,
			String identifier) {

		// get the concept attributes
		TkConceptAttributes attributes = concept.getConceptAttributes();
		if (attributes == null) {
			attributes = new TkConceptAttributes();
			concept.setConceptAttributes(attributes);
		}

		// get the additional ids list of the attributes
		List<TkIdentifier> additionalIds = attributes.additionalIds;
		if (additionalIds == null) {
			additionalIds = new ArrayList<TkIdentifier>();
			attributes.additionalIds = additionalIds;
		}

		// create the identifier and add it to the additional ids list
		EIdentifierString cid = new EIdentifierString();
		additionalIds.add(cid);

		// populate the identifier with the usual suspects
		cid.setAuthorityUuid(authorityUuid);
		cid.setPathUuid(this.pathUuid);
		cid.setStatusUuid(this.currentUuid);
		cid.setTime(System.currentTimeMillis());
		// populate the actual value of the identifier
		cid.setDenotation(identifier);
	}

	//
	// annotations
	//

	/**
	 * 
	 * Method to add an annotation to a component
	 * 
	 * @param component
	 * @param type
	 * @param annotationString
	 * @throws Exception
	 * 
	 */

	public void addAnnotation(TkComponent<?> component, UUID type,
			String annotationString) throws Exception {

		// get a time an the seqId of the annotation
		annotationCnt++;
		long time = System.currentTimeMillis();

		// get the annotations list for the component
		List<TkRefsetAbstractMember<?>> annotations = component
				.getAnnotations();
		if (annotations == null) {
			annotations = new ArrayList<TkRefsetAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		// create the TkRefsetStrMember and add it to the annotations list
		TkRefsetStrMember strRefexMember = new TkRefsetStrMember();
		annotations.add(strRefexMember);

		// set the primordial and time for the strRefexMember
		strRefexMember.setPrimordialComponentUuid(UUID
				.nameUUIDFromBytes(((this.uuidSeed + "." + annotationCnt)
						.getBytes())));
		strRefexMember.setTime(time);

		// set the constant uuids of the strRefexMember
		strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
		strRefexMember.setStatusUuid(this.currentUuid);
		strRefexMember.setAuthorUuid(this.autherUuid);
		strRefexMember.setPathUuid(this.pathUuid);

		// set the type and the value of the annotation
		strRefexMember.setStrValue(annotationString);
		strRefexMember.setRefsetUuid(type);

	}

	//
	// descriptions
	//

	/**
	 * 
	 * Method to add a description to a concept.
	 * 
	 * @param concept
	 * @param typeUuid
	 * @param descriptionStr
	 * 
	 */

	public void addDescription(EConcept concept, UUID typeUuid,
			String descriptionStr) {

		// get the descriptions
		List<TkDescription> descriptions = concept.descriptions;
		if (descriptions == null) {
			descriptions = new ArrayList<TkDescription>();
			concept.setDescriptions(descriptions);
		}

		// splSetId (preferredTerm)
		TkDescription description = new TkDescription();
		descriptions.add(description);
		description.setLang("en");
		description.setStatusUuid(currentUuid);
		description.setPrimordialComponentUuid(UUID.randomUUID());
		description.setConceptUuid(concept.primordialUuid);
		description.setPathUuid(this.pathUuid);
		description.setAuthorUuid(this.autherUuid);
		description.setTime(System.currentTimeMillis());
		description.setTypeUuid(typeUuid);
		description.text = descriptionStr;
	}

	public void addPreferredTerm(EConcept concept, String preferredTerm)
			throws Exception {
		UUID preferredTermUuid = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE
				.getPrimoridalUid();
		addDescription(concept, preferredTermUuid, preferredTerm);
	}

	//
	// relationships
	//

	/**
	 * 
	 * Method to add a relationship to a concept.
	 * 
	 * @param concept
	 * @param targetPrimordial
	 * @return
	 * @throws Exception
	 */

	public TkRelationship addRelationship(EConcept concept,
			UUID targetPrimordial) throws Exception {
		List<TkRelationship> relationships = concept.getRelationships();
		if (concept.getRelationships() == null) {
			relationships = new ArrayList<TkRelationship>();
			concept.setRelationships(relationships);
		}
		TkRelationship rel = this.createRelationship(concept, targetPrimordial);
		relationships.add(rel);
		return rel;
	}

	//
	// method to create the relationship
	//

	private TkRelationship createRelationship(EConcept eConcept,
			UUID targetPrimordial) throws IOException, TerminologyException {

		relCnt++;
		long time = System.currentTimeMillis();

		// uuids
		UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL
				.getPrimoridalUid();

		// create the relationship object
		TkRelationship rel = new TkRelationship();
		rel.setTime(time);
		rel.setRelGroup(0);
		rel.setPrimordialComponentUuid(UUID
				.nameUUIDFromBytes((this.uuidSeed + relCnt).getBytes()));
		rel.setC1Uuid(eConcept.getPrimordialUuid());
		rel.setTypeUuid(relPrimordial);
		rel.setC2Uuid(targetPrimordial);
		rel
				.setCharacteristicUuid(ArchitectonicAuxiliary.Concept.DEFINING_CHARACTERISTIC
						.getPrimoridalUid());
		rel.setRefinabilityUuid(ArchitectonicAuxiliary.Concept.NOT_REFINABLE
				.getPrimoridalUid());

		rel.setStatusUuid(currentUuid);
		rel.setAuthorUuid(this.autherUuid);
		rel.setPathUuid(this.pathUuid);

		return rel;

	}

	/**
	 * 
	 * Method to create a terminology auxiliary concept.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */

	public EConcept writeTerminologyAuxConcept(String name, DataOutputStream dos)
			throws Exception {
		System.out.println("CREATING AUX CON: " + name);
		UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT
				.getPrimoridalUid();
		return writeNamedConcept(archRoot, name, dos);
	}

}
