package gov.va.akcds.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.attribute.TkConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.media.TkMedia;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.cidcidcid.TkRefsetCidCidCidMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;
/**
 * Various constants and methods for building up workbench EConcepts.
 * @author Daniel Armbrust
 */
//TODO - this code is copy/paste inheritance from the NDF loader.  Really need to share this code....

public class EConceptUtility
{
	private final UUID author_ = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
	private final UUID currentUuid_ = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
	private final UUID path_ = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
	private final UUID preferredTerm_ = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
	private final UUID definingCharacteristic = ArchitectonicAuxiliary.Concept.DEFINING_CHARACTERISTIC.getPrimoridalUid();
	private final UUID notRefinable = ArchitectonicAuxiliary.Concept.NOT_REFINABLE.getPrimoridalUid();
	private final UUID isARel = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
	
	private final String lang_ = "en";
	
	//Used for making unique UUIDs
	private int relCounter_ = 0;
	private int annotationCounter_ = 0;
	private int descCounter_ = 0;
	private int mediaCounter_ = 0;
	private int tripleCounter_ = 0;
	
	private String uuidRoot_;

	public EConceptUtility(String uuidRoot) throws Exception
	{
		this.uuidRoot_ = uuidRoot;
	}

	public EConcept createConcept(UUID primordial, String preferredDescription, long time)
	{
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author_);
		conceptAttributes.setDefined(false);
		conceptAttributes.setPrimordialComponentUuid(primordial);
		conceptAttributes.setStatusUuid(currentUuid_);
		conceptAttributes.setPathUuid(path_);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);
		
		addDescription(concept, preferredDescription, preferredTerm_);

		return concept;
	}
	
	public TkDescription addDescription(EConcept concept, String descriptionValue,  UUID descriptionType)
	{
		List<TkDescription> descriptions = concept.getDescriptions();
		if (descriptions == null)
		{
			descriptions = new ArrayList<TkDescription>();
			concept.setDescriptions(descriptions);
		}
		TkDescription description = new TkDescription();
		description.setConceptUuid(concept.getPrimordialUuid());
		description.setLang(lang_);
		description.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "descr:" + descCounter_++).getBytes()));
		description.setTypeUuid(descriptionType);
		description.setText(descriptionValue);
		description.setStatusUuid(currentUuid_);
		description.setAuthorUuid(author_);
		description.setPathUuid(path_);
		description.setTime(System.currentTimeMillis());

		descriptions.add(description);
		return description;
	}
	
	public EIdentifierString addAdditionalId(EConcept concept, Object denotation, UUID authorityUUID)
	{
		if (denotation != null)
		{
			List<TkIdentifier> additionalIds = concept.getConceptAttributes().getAdditionalIdComponents();
			if (additionalIds == null)
			{
				additionalIds = new ArrayList<TkIdentifier>();
				concept.getConceptAttributes().setAdditionalIdComponents(additionalIds);
			}

			// create the identifier and add it to the additional ids list
			EIdentifierString cid = new EIdentifierString();
			additionalIds.add(cid);

			// populate the identifier with the usual suspects
			cid.setAuthorityUuid(authorityUUID);
			cid.setPathUuid(path_);
			cid.setStatusUuid(currentUuid_);
			cid.setTime(System.currentTimeMillis());
			// populate the actual value of the identifier
			cid.setDenotation(denotation);
			return cid;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(TkComponent<?> component, String value, UUID refsetUUID)
	{
		List<TkRefsetAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefsetAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefsetStrMember strRefexMember = new TkRefsetStrMember();

			strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			strRefexMember.setStrValue(value);
			strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "annotation:" + annotationCounter_++).getBytes()));
			strRefexMember.setRefsetUuid(refsetUUID);
			strRefexMember.setStatusUuid(currentUuid_);
			strRefexMember.setAuthorUuid(author_);
			strRefexMember.setPathUuid(path_);
			strRefexMember.setTime(System.currentTimeMillis());
			annotations.add(strRefexMember);

			return strRefexMember;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(EConcept concept, String value, UUID refsetUUID)
	{
		TkConceptAttributes conceptAttributes = concept.getConceptAttributes();
		return addAnnotation(conceptAttributes, value, refsetUUID);
	}
	
	public TkRefsetCidCidCidMember addCIDTriple(EConcept concept, UUID c1, UUID c2, UUID c3, UUID refsetUUID)
	{
		TkConceptAttributes attributes = concept.getConceptAttributes();
		List<TkRefsetAbstractMember<?>> annotations = attributes.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefsetAbstractMember<?>>();
			attributes.setAnnotations(annotations);
		}
		
		TkRefsetCidCidCidMember triple = new TkRefsetCidCidCidMember();
		triple.setRefsetUuid(refsetUUID);
		triple.setTime(System.currentTimeMillis());
		triple.setStatusUuid(currentUuid_);
		triple.setComponentUuid(attributes.getPrimordialComponentUuid());
		triple.setPathUuid(path_);
		triple.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "triple:" + tripleCounter_++).getBytes()));
		triple.setAuthorUuid(author_);
		triple.setC1Uuid(c1);
		triple.setC2Uuid(c2);
		triple.setC3Uuid(c3);
		
		annotations.add(triple);
		
		return triple;
	}
	
	/**
	 * relationshipPrimoridal is optional - if not provided, the default value of IS_A_REL is used.
	 */
	public TkRelationship addRelationship(EConcept concept, UUID targetPrimordial, UUID relationshipPrimoridal) 
	{
		List<TkRelationship> relationships = concept.getRelationships();
		if (relationships == null)
		{
			relationships = new ArrayList<TkRelationship>();
			concept.setRelationships(relationships);
		}
		 
		TkRelationship rel = new TkRelationship();
		rel.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "rel" + relCounter_++).getBytes()));
		rel.setC1Uuid(concept.getPrimordialUuid());
		rel.setTypeUuid(relationshipPrimoridal == null ? isARel : relationshipPrimoridal);
		rel.setC2Uuid(targetPrimordial);
		rel.setCharacteristicUuid(definingCharacteristic);
		rel.setRefinabilityUuid(notRefinable);
		rel.setStatusUuid(currentUuid_);
		rel.setAuthorUuid(author_);
		rel.setPathUuid(path_);
		rel.setTime(System.currentTimeMillis());
		rel.setRelGroup(0);  

		relationships.add(rel);
		return rel;
	}
	
	/**
	 * Attach an image... format is a rather mysterious concept in this data model... try sending in just the extension
	 * that would typically be part of the file name which stores the image.
	 */
	public void addMedia(EConcept concept, UUID type, byte[] imageData, String format, String description) throws Exception
	{
		TkMedia newMedia = new TkMedia();
		newMedia.setDataBytes(imageData);
		
		// set the primordial and time
		newMedia.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "media:" + mediaCounter_++).getBytes()));
		newMedia.setTime(System.currentTimeMillis());

		// set the constant uuids 
		newMedia.setStatusUuid(currentUuid_);
		newMedia.setAuthorUuid(author_);
		newMedia.setPathUuid(path_);

		newMedia.setTypeUuid(ArchitectonicAuxiliary.Concept.AUXILLARY_IMAGE.getPrimoridalUid());
		newMedia.setConceptUuid(concept.getPrimordialUuid());
		newMedia.setFormat(format);
		newMedia.setTextDescription(description);
		
		List<TkMedia> images = concept.getImages();
		if (images == null)
		{
			images = new ArrayList<TkMedia>();
		}
		images.add(newMedia);
		concept.setImages(images);
	}
}
