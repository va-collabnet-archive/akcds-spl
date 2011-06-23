package gov.va.akcds.splMojo.model;

public class SimpleDraftFactSource
{
	public String rowId;
	public String setId;
	public String sectionName;
	public String sentenceContext;
	public String ndc;
	public String curationComment;
	
	public SimpleDraftFactSource(String rowId, String setId, String sectionName, String sentenceContext, String ndc)
	{
		this.rowId = rowId;
		this.setId = setId;
		this.sectionName = sectionName;
		this.sentenceContext = sentenceContext;
		this.ndc = ndc;
	}
}
