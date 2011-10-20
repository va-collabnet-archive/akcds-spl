package gov.va.akcds.splMojo.model;

import java.util.ArrayList;
import java.util.UUID;

public class SimpleDraftFact
{
	public String drugName;
	public String relName;
	public String targetCode;
	public String targetCodeName;
	public UUID targetCodeUUID;
	public String curationState;
	
	
	public ArrayList<SimpleDraftFactSource> sources = new ArrayList<SimpleDraftFactSource>();
	
	public SimpleDraftFact(String drugName, String relName, String targetCode)
	{
		this.drugName = drugName.trim();
		this.relName = relName.toUpperCase().trim();
		this.targetCode = targetCode.toUpperCase().trim();  //this may be a name, in some cases.
	}

	public String getUniqueKey()
	{
		return drugName + ":" + relName +  ":" + targetCode;
	}
	
	
}
