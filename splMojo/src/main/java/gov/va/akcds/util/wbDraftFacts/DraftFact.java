package gov.va.akcds.util.wbDraftFacts;

public class DraftFact {

	public DraftFact(String initString) {
		String[] tokens = initString.split("\t");

		this.rowId = tokens[0];
		this.splSetId = tokens[1];
		this.splDirName = tokens[2];
		this.splFileName = tokens[3];
		this.drugRoleConceptId = tokens[4];
		this.docId = tokens[5];
		this.secId = tokens[6];
		this.secName = tokens[7];
		this.drugCode = tokens[8];
		this.roleId = tokens[9];
		this.conceptCode = tokens[10];
		this.drugName = tokens[11];
		this.roleName = tokens[12];
		this.conceptName = tokens[13];
		this.sentenceId = tokens[14];
		this.phraseId = tokens[15];
		this.ceId = tokens[16];
		this.sequenceId = tokens[17];
		this.distinction = tokens[18];
		this.aggregateionConceptId = tokens[19];
		this.aggregationConceptName = tokens[20];
		this.newFileName = tokens[21];
		this.sentence = tokens[22];
		this.sentenceNumber = tokens[23];
	}
	
	private String rowId;

	private String splSetId;
	
	private String splDirName;
	
	private String splFileName;
	
	private String drugRoleConceptId;
	
	private String docId;
	
	private String secId;
	
	private String secName;
	
	private String drugCode;
	
	private String roleId;
	
	private String conceptCode;
	
	private String drugName;
	
	private String roleName;
	
	private String conceptName;
	
	private String sentenceId;
	
	private String phraseId;
	
	private String ceId;
	
	private String sequenceId;
	
	private String distinction;
	
	private String aggregateionConceptId;
	
	private String aggregationConceptName;
	
	private String newFileName;
	
	private String sentence;
	
	private String sentenceNumber;

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String getSplSetId() {
		return splSetId;
	}

	public void setSplSetId(String splSetId) {
		this.splSetId = splSetId;
	}

	public String getSplDirName() {
		return splDirName;
	}

	public void setSplDirName(String splDirName) {
		this.splDirName = splDirName;
	}

	public String getSplFileName() {
		return splFileName;
	}

	public void setSplFileName(String splFileName) {
		this.splFileName = splFileName;
	}

	public String getDrugRoleConceptId() {
		return drugRoleConceptId;
	}

	public void setDrugRoleConceptId(String drugRoleConceptId) {
		this.drugRoleConceptId = drugRoleConceptId;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public String getSecId() {
		return secId;
	}

	public void setSecId(String secId) {
		this.secId = secId;
	}

	public String getSecName() {
		return secName;
	}

	public void setSecName(String secName) {
		this.secName = secName;
	}

	public String getDrugCode() {
		return drugCode;
	}

	public void setDrugCode(String drugCode) {
		this.drugCode = drugCode;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getConceptCode() {
		return conceptCode;
	}

	public void setConceptCode(String conceptCode) {
		this.conceptCode = conceptCode;
	}

	public String getDrugName() {
		return drugName;
	}

	public void setDrugName(String drugName) {
		this.drugName = drugName;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getConceptName() {
		return conceptName;
	}

	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}

	public String getSentenceId() {
		return sentenceId;
	}

	public void setSentenceId(String sentenceId) {
		this.sentenceId = sentenceId;
	}

	public String getPhraseId() {
		return phraseId;
	}

	public void setPhraseId(String phraseId) {
		this.phraseId = phraseId;
	}

	public String getCeId() {
		return ceId;
	}

	public void setCeId(String ceId) {
		this.ceId = ceId;
	}

	public String getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(String sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getDistinction() {
		return distinction;
	}

	public void setDistinction(String distinction) {
		this.distinction = distinction;
	}

	public String getAggregateionConceptId() {
		return aggregateionConceptId;
	}

	public void setAggregateionConceptId(String aggregateionConceptId) {
		this.aggregateionConceptId = aggregateionConceptId;
	}

	public String getAggregationConceptName() {
		return aggregationConceptName;
	}

	public void setAggregationConceptName(String aggregationConceptName) {
		this.aggregationConceptName = aggregationConceptName;
	}

	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String getSentenceNumber() {
		return sentenceNumber;
	}

	public void setSentenceNumber(String sentenceNumber) {
		this.sentenceNumber = sentenceNumber;
	}

}
