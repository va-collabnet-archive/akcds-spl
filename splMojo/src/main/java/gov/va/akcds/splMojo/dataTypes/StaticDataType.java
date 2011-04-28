package gov.va.akcds.splMojo.dataTypes;

import gov.va.akcds.splMojo.SplMojo;

import java.util.UUID;

public enum StaticDataType
{
	SET_ID("Set ID", "An optional globally-unique identifier, that remains constant across all document revisions that derive"
			+ " from a common original document."), 
	DRAFT_FACT("Draft Fact", ""), 
	DRUG_NAME("Drug Name", "The drug name from the NDC"), 
	DRUG_CODE("Drug Code", "The NDC drug code"),
	SPL_XML_TEXT("SPL XML Text", "The entire SPL XML document."), 
	IMAGE("Image", "An image that is referenced from the SPL XML document"), 
	VERSION("Version",
			"An optional version number is incremented for each subsequent version of a document. For example, an original document"
			+ " is the first version of a document, and gets a new globally unique \"id\" value; it can also contain a new value for"
			+ " \"setId\" and a value of \"versionNumber\" set to equal \"1\"."
			), 
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
