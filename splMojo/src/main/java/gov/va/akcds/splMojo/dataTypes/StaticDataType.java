package gov.va.akcds.splMojo.dataTypes;

import gov.va.akcds.splMojo.SplMojo;

import java.util.UUID;

public enum StaticDataType
{
	SET_ID("Set ID", "An optional globally-unique identifier, that remains constant across all document revisions that derive"
			+ " from a common original document."),
	SPL_VERSION("SPL version", "The SPL version number"),
	DRAFT_FACT_TRIPLE("Draft Fact Triple", "Draft Fact Triple"),
	DRAFT_FACT_SENTENCE("Draft Fact Sentence", "The source of the draft fact"),
	DRAFT_FACT_SEC_NAME("Draft Fact Section Name", "The section source of the draft fact"),
	DRAFT_FACT_UNIQUE_ID("Draft Fact Unique ID", "A unique value we have assigned to this draft fact"),
	DRAFT_FACT_DRUG_NAME("Drug Name", "The drug name from the NDC"), 
	DRAFT_FACT_DRUG_CODE("NDC", "The New Drug Code"),
	DRAFT_FACT_SNOMED_CONCEPT_NAME("Snomed Concept Name", "The name of the snomed concept code"),
	DRAFT_FACT_SNOMED_CONCEPT_CODE("Snomed Concept Code", "The snomed concept code"),
	DRAFT_FACT_TRUE("True", "Draft facts somtimes contain true/false instead of snomed concept code / name"),
	DRAFT_FACT_FALSE("False", "Draft facts somtimes contain true/false instead of snomed concept code / name"),
	DRAFT_FACT_CURATION_STATE("Curation state", "Draft fact curation state"),
	DRAFT_FACT_COMMENT("Curation comment", "Draft fact curation comment"),
	SPL_XML_TEXT("SPL XML Text", "The entire SPL XML document."), 
	IMAGE("Image", "An image that is referenced from the SPL XML document"), 
	VERSION("Version","Version number of the SPL data load"), 
	APPROVAL_NUMBER("Approval Number", "Approval number (which in the U.S. is the New Drug Application (NDA) number)");
	

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
