package gov.va.akcds.util.wbDraftFacts;


public class DraftFact {
	public DraftFact(String initString, String fromFileName) {
		String[] tokens = initString.split("\t");

		this.fromFileName = fromFileName;
		
		this.rowId = tokens[0];
		this.splSetId = tokens[1].toUpperCase();
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
		
		if (tokens.length > 24)
		{
			//BW data has these extras, we should have all 3.
			this.curationState = tokens[24];
			this.comment = tokens[25];
			this.splVersion = tokens[26];
		}
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
	
	private String curationState;
	private String comment;
	private String splVersion;
	
	private String fromFileName;

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
		this.splSetId = splSetId.toUpperCase();
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

	public String getCurationState() {
		return curationState;
	}

	public void setCurationState(String curationState) {
		this.curationState = curationState;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getSplVersion() {
		if (splVersion == null)
		{
			splVersion = "-";
		}
		return splVersion;
	}

	public void setSplVersion(String version) {
		this.splVersion = version;
	}
	
	public String getFromFileName()
	{
		return fromFileName;
	}
	
	/**
	 * Dump it back out in the format we read it in.
	 * @return
	 */
	public String format()
	{
		StringBuilder sb = new StringBuilder();
		if (this.fromFileName != null && this.fromFileName.length() > 0)
		{
			sb.append(this.fromFileName);
			sb.append("\t");
		}
		sb.append(this.rowId);
		sb.append("\t");
		sb.append(this.splSetId);
		sb.append("\t");
	
		sb.append(this.splDirName);
		sb.append("\t");
		sb.append(this.splFileName);
		sb.append("\t");
		sb.append(this.drugRoleConceptId);
		sb.append("\t");
		sb.append(this.docId);
		sb.append("\t");
		sb.append(this.secId);
		sb.append("\t");
		sb.append(this.secName);
		sb.append("\t");
		sb.append(this.drugCode);
		sb.append("\t");
		sb.append(this.roleId);
		sb.append("\t");
		sb.append(this.conceptCode);
		sb.append("\t");
		sb.append(this.drugName);
		sb.append("\t");
		sb.append(this.roleName);
		sb.append("\t");
		sb.append(this.conceptName);
		sb.append("\t");
		sb.append(this.sentenceId);
		sb.append("\t");
		sb.append(this.phraseId);
		sb.append("\t");
		sb.append(this.ceId);
		sb.append("\t");
		sb.append(this.sequenceId);
		sb.append("\t");
		sb.append(this.distinction);
		sb.append("\t");
		sb.append(this.aggregateionConceptId);
		sb.append("\t");
		sb.append(this.aggregationConceptName);
		sb.append("\t");
		sb.append(this.newFileName);
		sb.append("\t");
		sb.append(this.sentence);
		sb.append("\t");
		sb.append(this.sentenceNumber);
		sb.append("\t");
		sb.append(this.curationState);
		sb.append("\t");
		sb.append(this.comment);
		sb.append("\t");
		sb.append(this.splVersion);
		sb.append("\t");

		return sb.toString();
	}
}
