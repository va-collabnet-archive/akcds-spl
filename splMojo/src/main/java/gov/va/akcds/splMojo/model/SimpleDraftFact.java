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
		this.drugName = drugName;
		this.relName = relName;
		this.targetCode = targetCode;
	}

	public String getUniqueKey()
	{
		return drugName + ":" + relName +  ":" + targetCode;
	}
	
	
}
