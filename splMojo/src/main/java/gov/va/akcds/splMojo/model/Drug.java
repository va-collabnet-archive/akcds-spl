package gov.va.akcds.splMojo.model;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.UUID;

public class Drug
{
	public String drugName;
	public HashSet<UUID> setIds = new HashSet<UUID>();
	
	public Hashtable<String, SimpleDraftFact> draftFacts = new Hashtable<String, SimpleDraftFact>();
	
	
	public Drug(String drugName)
	{
		this.drugName = drugName;
	}
}
