package gov.va.akcds.splMojo.model;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.UUID;

public class Drug
{
	public String drugName;
	public HashSet<UUID> setIdUUIDs = new HashSet<UUID>();
	public HashSet<String> setIds = new HashSet<String>();
	public HashSet<String> rxNormVuids = new HashSet<String>();
	public HashSet<String> rxNormTradeNameVuids = new HashSet<String>();
	
	public Hashtable<String, SimpleDraftFact> draftFacts = new Hashtable<String, SimpleDraftFact>();
	
	
	public Drug(String drugName)
	{
		this.drugName = drugName;
	}
}
