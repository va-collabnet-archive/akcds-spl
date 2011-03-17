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
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

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

	/**
	 * 
	 * Method to create a named concept.
	 * 
	 * @param name
	 * @return
	 * 
	 */

	public EConcept createNamedEConcept(String name) {
		// create the concept
		System.out.println("Writing SPL ROOT CONCEPT:");
		UUID primordial = UUID.nameUUIDFromBytes((this.uuidSeed + "." + name)
				.getBytes());
		EConcept concept = newInstance(primordial);
		addDescription(concept, this.preferredTermUuid, "SPL");
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

	public EConcept writeNamedEConcept(String name, DataOutputStream dos)
			throws Exception {
		EConcept concept = createNamedEConcept(name);
		concept.writeExternal(dos);
		dos.flush();
		return concept;
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

	//
	// relationships
	//

	/**
	 * 
	 * Method to create a relationship.
	 * 
	 * @param eConcept
	 * @param targetPrimordial
	 * @return
	 * @throws IOException
	 * @throws TerminologyException
	 * 
	 */

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

	/**
	 * 
	 * Method to create a terminology auxiliary concept.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */

	public EConcept createTerminologyAuxConcept(String name) throws Exception {

		long time = System.currentTimeMillis();
		UUID primordial = UUID.nameUUIDFromBytes((this.uuidSeed + "." + name)
				.getBytes());

		UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT
				.getPrimoridalUid();

		EConcept concept = this.createNamedEConcept(name);
		System.out.println("CREATING AUX CON: " + name);

		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(this.autherUuid);

		conceptAttributes = new EConceptAttributes();
		conceptAttributes.defined = false;
		conceptAttributes.primordialUuid = primordial;
		conceptAttributes.statusUuid = currentUuid;
		conceptAttributes.setPathUuid(this.pathUuid);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);

		List<TkDescription> descriptions = new ArrayList<TkDescription>();
		TkDescription description = new TkDescription();
		description.setConceptUuid(primordial);
		description.setLang("en");
		description.setPrimordialComponentUuid(UUID.randomUUID());

		description.setTypeUuid(this.preferredTermUuid);
		description.text = name;
		description.setStatusUuid(currentUuid);
		description.setAuthorUuid(this.autherUuid);
		description.setPathUuid(this.pathUuid);
		description.setTime(time);
		descriptions.add(description);
		concept.setDescriptions(descriptions);

		List<TkRelationship> relationships = new ArrayList<TkRelationship>();
		TkRelationship heirRel = createRelationship(concept, archRoot);
		relationships.add(heirRel);
		concept.setRelationships(relationships);

		System.out.println("Wrote: " + concept);
		return concept;
	}

}
