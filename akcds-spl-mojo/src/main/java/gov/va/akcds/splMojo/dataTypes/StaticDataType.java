package gov.va.akcds.splMojo.dataTypes;

import gov.va.akcds.splMojo.SplMojo;

import java.util.UUID;

public enum StaticDataType
{
	SPL_SET_ID("Set ID", "An optional globally-unique identifier, that remains constant across all document revisions that derive"
			+ " from a common original document."),
	SPL_VERSION("SPL version", "The SPL version number"),
	DRAFT_FACT_TRIPLE("Draft Fact Triple", "Draft Fact Triple"),
	DRAFT_FACT_SENTENCE("Draft Fact Sentence", "The source of the draft fact"),
	DRAFT_FACT_SEC_NAME("Draft Fact Section Name", "The section source of the draft fact"),
	DRAFT_FACT_SET_ID("Draft Fact Set ID", "The set id for the fact"),
	DRAFT_FACT_UNIQUE_ID("Draft Fact Unique ID", "A unique value we have assigned to this draft fact"),
	SUPER_DRAFT_FACT_UNIQUE_ID("Super Draft Fact Unique ID", "A unique value we have assigned to this draft fact (which is a merger of one or more draft facts)"),
	DRAFT_FACT_DRUG_CODE("NDC", "The New Drug Code"),
	DRAFT_FACT_SNOMED_CONCEPT_NAME("Snomed Concept Name", "The name of the snomed concept code"),
	DRAFT_FACT_SNOMED_CONCEPT_CODE("Snomed Concept Code", "The snomed concept code"),
	DRAFT_FACT_TRUE("True", "Draft facts somtimes contain true/false instead of snomed concept code / name"),
	DRAFT_FACT_FALSE("False", "Draft facts somtimes contain true/false instead of snomed concept code / name"),
	DRAFT_FACT_CURATION_STATE("Curation state", "Draft fact curation state"),
	DRAFT_FACT_COMMENT("Curation comment", "Draft fact curation comment"),
	SPL_XML_TEXT("SPL XML Text", "The entire SPL XML document."), 
	SPL_IMAGE("Image", "An image that is referenced from the SPL XML document"),
	RXNorm_VUID("rxNorm VUID", "A VUID from rxNorm for this drug concept"), 
	RXNorm_VUID_TRADENAME("rxNorm Tradename VUID", "A VUID from rxNorm for a tradename_of relation of this drug concept"), 
	VERSION("Version","Version number of the SPL data load");
	

	private String name_;
	private String description_;
	private UUID uuid_;

	private StaticDataType(String name, String description)
	{
		this.name_ = name;
		this.uuid_ = UUID.nameUUIDFromBytes((SplMojo.uuidRoot_ + ":metadata:types:" + name_).getBytes());
		this.description_ = description;
	}

	public String getNiceName()
	{
		return name_;
	}

	public String getDescription()
	{
		return description_;
	}

	public UUID getUuid()
	{
		return uuid_;
	}
}
