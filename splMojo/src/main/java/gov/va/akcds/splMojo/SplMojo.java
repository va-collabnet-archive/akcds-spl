package gov.va.akcds.splMojo;

/**
 * In general, this tool simply loads the SPL documents provided in the zip file - combining them with draft facts.
 * 
 * However, there are a couple of additional filter steps:
 * 
 * We toss any document where we don't have any draft facts.
 * We toss any NDAs that start with 'ANADA', 'NADA', or 'part'
 * If there are no remaining NDAs left on the SPL document at that point, we toss the entire document, 
 * even if we have draft facts for that document.
 */

import gov.va.akcds.spl.NDA;
import gov.va.akcds.spl.Spl;
import gov.va.akcds.splMojo.dataTypes.DynamicDataType;
import gov.va.akcds.splMojo.dataTypes.StaticDataType;
import gov.va.akcds.util.EConceptUtility;
import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;
import gov.va.akcds.util.zipUtil.ZipContentsIterator;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.util.id.Type3UuidFactory;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.refset.cidcidcid.TkRefsetCidCidCidMember;

/**
 * Goal to generate spl data file
 * 
 * @goal convert-spl-to-jbin
 * 
 * @phase process-sources
 */

public class SplMojo extends AbstractMojo
{

	public static final String uuidRoot_ = "gov.va.spl";

	/**
	 * Draft facts parent dir.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;
	
	/**
	 * Location of the file.
	 * 
	 * @parameter
	 * @required
	 */
	private File output;
	
	/**
	 * Location of the data.
	 * 
	 * @parameter
	 * @required
	 */
	private File[] facts;
	
	/**
	 * Location of the data.
	 * 
	 * @parameter
	 * @required
	 */
	private boolean filterNda;
	
	
	/**
	 * Location of the source.
	 * 
	 * @parameter
	 * @required
	 */
	private File zips;

	private EConceptUtility conceptUtility_ = new EConceptUtility(uuidRoot_);

	private DraftFacts draftFacts;
	
	private Hashtable<String, DynamicDataType> dynamicDataTypes_ = new Hashtable<String, DynamicDataType>();
	private Hashtable<Integer, UUID> letterRoots_ = new Hashtable<Integer, UUID>();
	private UUID nonSnomed;
	private UUID ndaTypeRoot_, draftFactsRoot_;

	private DataOutputStream dos_;
	private long metadataConceptCounter_ = 0;
	private long conceptCounter_ = 0;
	private long xmlFileCnt_ = 0;
	private ArrayList<String> dropForNoFacts_ = new ArrayList<String>();
	private ArrayList<String> dropForNoNDAs_ = new ArrayList<String>();
	
	private boolean createLetterRoots_ = true;  //switch this to false to generate a flat structure under the spl root concept
	
	private Map<UUID, ConceptData> conMap = new HashMap<UUID, ConceptData>();
	
	private Set<UUID> nonSnomedTerms =new HashSet<UUID>();
	
	class ConceptData 
	{
		EConcept con;
		String drugName;
		Set<String> xmlFiles = new HashSet<String>();
		Set<String> imgFiles = new HashSet<String>();
		Set<String> nda = new HashSet<String>();
		
		
		public ConceptData(EConcept con, String drugName)
		{
			this.con = con;
			this.drugName=drugName; 
		}
		
	}

	public SplMojo() throws Exception
	{
	}
	
	/**
	 * Method used by maven to create the .jbin data file.
	 */
	public void execute() throws MojoExecutionException
	{

		try
		{
			// echo status
			System.out.println("TPS report completed 04/05/2011.");
			System.out.println("Starting creation of .jbin file for Structured Product Labels (SPLs)");
			System.out.println(new Date().toString());

			// output directory
			System.out.println(getOutput());
			if (getOutput().getParentFile().exists() == false)
			{
				getOutput().getParentFile().mkdirs();
			}

			// jbin (output) file
			File jbinFile = getOutput();
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(jbinFile)));

			createMetaData();

			// Create the root concept (named SPL)
			EConcept rootConcept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root").getBytes()), "SPL",
					System.currentTimeMillis());
			conceptUtility_.addDescription(rootConcept, "1.0",  StaticDataType.VERSION.getUuid());
			storeConcept(rootConcept);
			metadataConceptCounter_++;			
			
			// Create the root concept (named non-snomed)
			EConcept nonSnoConcept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root:non-snomed").getBytes()), "~non-SNOMED CT", System.currentTimeMillis());
			nonSnomed = nonSnoConcept.getPrimordialUuid();
			conceptUtility_.addDescription(nonSnoConcept, "1.0",  StaticDataType.VERSION.getUuid());
			// adding as child as it doesn'e seem to show up as a root concept?
			conceptUtility_.addRelationship(nonSnoConcept, rootConcept.getPrimordialUuid(), null);
			storeConcept(nonSnoConcept);
			metadataConceptCounter_++;			

			
			if (createLetterRoots_)
			{
				//Set up letter roots to organize the labels
				for (int i = 65; i <= 90; i++)
				{
					String s = new String(Character.toChars(i));					
					EConcept concept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root:" + s).getBytes()), s, System.currentTimeMillis());
					letterRoots_.put(i, concept.getPrimordialUuid());
					conceptUtility_.addRelationship(concept, rootConcept.getPrimordialUuid(), null);
					storeConcept(concept);
					metadataConceptCounter_++;
				}	
			}
			
			
			System.out.println();
			System.out.println("Created " + metadataConceptCounter_ + " initial metadata concepts");

			
			// load the draft facts
			System.out.println("Loading draft facts:");
			File[] draftFactsFile = getFacts();			
			draftFacts = new DraftFacts(draftFactsFile, new File(outputDirectory, "draftFactsByID"));

			// source file (splSrcData.zip is a zip of zips)
			File dataFile= getZips();
			
			System.out.println(new Date().toString());
			System.out.println("Reading spl zip file : "+dataFile);
			// process the zip of zips
			ZipContentsIterator outerZCI = new ZipContentsIterator(dataFile);

			while (outerZCI.hasMoreElements())
			{
				// Each of these should be a zip file
				ZipFileContent nestedZipFile = outerZCI.nextElement();

				if (nestedZipFile.getName().toLowerCase().endsWith(".zip"))
				{
					// open up the nested zip file
					ZipContentsIterator nestedZipFileContents = new ZipContentsIterator(nestedZipFile.getFileBytes());

					ArrayList<ZipFileContent> filesInNestedZipFile = new ArrayList<ZipFileContent>();

					while (nestedZipFileContents.hasMoreElements())
					{
						filesInNestedZipFile.add(nestedZipFileContents.nextElement());
					}

					if (filesInNestedZipFile.size() > 0)
					{
						// Pass the elements in to the spl factory
						Spl spl = new Spl(filesInNestedZipFile, nestedZipFile.getName());
						writeEConcept(spl, rootConcept.getPrimordialUuid(),  nestedZipFile.getName());
					}
					else
					{
						System.err.println("Empty inner zip file? " + nestedZipFile.getName());
					}
				}
				else
				{
					System.err.println("Skipping unexpected file in outer zip file: " + nestedZipFile.getName());
				}

				xmlFileCnt_++;
			}

			System.out.println();

			dos_.flush();
			dos_.close();						
			
			// write the meta data concepts
			System.out.println("TOTAL SPL FILES:   " + xmlFileCnt_);
			System.out.println("TOTAL concepts created: " + conceptCounter_);
			System.out.println("Metadata concepts created: " + metadataConceptCounter_);
			System.out.println("SPL concepts created: " + (conceptCounter_ - metadataConceptCounter_));
			System.out.println("Ignored " + dropForNoFacts_.size() + " files for not having any draft facts");
			System.out.println("Ignored " + dropForNoNDAs_.size() + " files for not having any NDAs");						

		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private void createMetaData() throws Exception
	{
		// Set up a meta-data root concept
		UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
		UUID metaDataRoot = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata").getBytes());
		writeAuxEConcept(metaDataRoot, "SPL Metadata", "", archRoot);

		UUID typesRoot = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types").getBytes());
		writeAuxEConcept(typesRoot, "Types", "", metaDataRoot);
		
		ndaTypeRoot_ = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types:ndaTypes").getBytes());
		writeAuxEConcept(ndaTypeRoot_, "NDA Types", "", typesRoot);
		
		draftFactsRoot_ = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types:draftFactsRelationships").getBytes());
		writeAuxEConcept(draftFactsRoot_, "Draft Fact Relationships", "", typesRoot);

		for (StaticDataType dt : StaticDataType.values())
		{
			writeAuxEConcept(dt.getUuid(), dt.getNiceName(), dt.getDescription(), typesRoot);
		}
	}
	
	private EConcept initialiseConcept(DraftFact fact, Spl spl, UUID rootConceptUUID) throws Exception
	{
		String drugName = fact.getDrugName().toUpperCase();
		UUID key = UUID.nameUUIDFromBytes((uuidRoot_ + ":" + drugName+":"+spl.getSetId()+":"+spl.getVersion()).getBytes());
		ConceptData conceptData = conMap.get(key);
		
		if (conceptData == null)
		{			
			EConcept concept = conceptUtility_.createConcept(key,drugName, System.currentTimeMillis());
			conceptData = new ConceptData(concept, drugName);
			conMap.put(key, conceptData);
			conceptUtility_.addAdditionalId(concept, spl.getSetId(), StaticDataType.SET_ID.getUuid());
			conceptUtility_.addAdditionalId(concept, fact.getDrugCode(), StaticDataType.DRAFT_FACT_DRUG_CODE.getUuid());		
			conceptUtility_.addDescription(concept, drugName, StaticDataType.DRAFT_FACT_DRUG_NAME.getUuid());
			conceptUtility_.addAnnotation(concept, spl.getVersion(), StaticDataType.SPL_VERSION.getUuid());						
			
			//Find the right letter parent to to place it under.
			UUID parentUUID = null;
			
			if (createLetterRoots_)
			{
			
				for (int i = 0; i < drugName.length(); i++)
				{
					if (parentUUID != null)
					{
						break;
					}
					parentUUID = letterRoots_.get(drugName.codePointAt(i));
				}

				if (parentUUID == null)
				{
					//Still null?  No letters in the name?
					parentUUID = rootConceptUUID;
				}
			}
			else
			{
				parentUUID = rootConceptUUID;
			}
			
			conceptUtility_.addRelationship(concept, parentUUID, null);
			
		}
		
		if (!conceptData.xmlFiles.contains(spl.getXmlFileName()))
		{
			conceptData.xmlFiles.add(spl.getXmlFileName());
			conceptUtility_.addAnnotation(conceptData.con, spl.getXMLFileAsString(), StaticDataType.SPL_XML_TEXT.getUuid());
		}
		

		ArrayList<ZipFileContent> media = spl.getSupportingFiles();
		for (ZipFileContent zfc : media)
		{
			String fileName = zfc.getName();
			if (!conceptData.imgFiles.contains(fileName))
			{			
				conceptData.imgFiles.add(fileName);
				int splitPos = fileName.lastIndexOf('.');
				String extension = ((splitPos + 1 <= fileName.length() ? fileName.substring(splitPos + 1) : ""));
				conceptUtility_.addMedia(conceptData.con, StaticDataType.IMAGE.getUuid(), zfc.getFileBytes(), extension, fileName);
			}
		}
		
		for (NDA nda : spl.getUniqueNDAs())
		{
			DynamicDataType ddt = getNDAType(nda.getType());
			if (!conceptData.nda.contains(nda.getValue()))
			{
				conceptData.nda.add(nda.getValue());
				conceptUtility_.addAnnotation(conceptData.con, nda.getValue(), ddt.getIdentifier());
			}
		}

		
		return conceptData.con;
	}
	
	private void writeEConcept(Spl spl, UUID rootConceptUUID, String nestedZipFile) throws Exception
	{
		conMap.clear();
		
		// get the facts
		ArrayList<DraftFact> splDraftFacts = draftFacts.getFacts(spl.getSetId());
				
		if (splDraftFacts.size() == 0 || (isFilterNda() && !spl.hasAtLeastOneNDA()))
		{
			// if there are no facts don't add the spl (it complicates things)
			// also drop anything with no nda values
			if (splDraftFacts.size() == 0)
			{
				dropForNoFacts_.add(spl.getSetId());
			}
			else
			{
				dropForNoNDAs_.add(spl.getSetId());
			}
			return;
		}

		// add an annotation to the conceptAttributes for each draft fact
		if (splDraftFacts != null)
		{
			for (int i = 0; i < splDraftFacts.size(); i++)
			{
				
				DraftFact fact = splDraftFacts.get(i);

				if (fact.getDrugName().equalsIgnoreCase("ALCOHOL"))
				{
					System.out.println(fact.getDrugName());
				}
				
				if (fact.getSplVersion().equals("-"))
				{
					fact.setSplVersion(spl.getVersion());
				}
				
				if (!fact.getSplVersion().equals(spl.getVersion()))
				{
					continue;
				}
				
				EConcept concept = initialiseConcept(fact, spl, rootConceptUUID);
				UUID draftFactRoleUUID = getDraftFactType(fact.getRoleName()).getIdentifier();
				UUID draftFactTargetUUID = null;
				
				//This is an oddity discovered later in the draft facts... which wasn't documented.  Not sure what the correct solution is, 
				//but will do this for now....
								
				if (fact.getConceptCode().equals("1"))
				{
					draftFactTargetUUID = StaticDataType.DRAFT_FACT_TRUE.getUuid();
				}
				else if (fact.getConceptCode().equals("0"))
				{
					draftFactTargetUUID = StaticDataType.DRAFT_FACT_FALSE.getUuid();
				}				
				else if (fact.getConceptCode().equals("-"))
				{
					draftFactTargetUUID = UUID.nameUUIDFromBytes((uuidRoot_ + ":"+ spl.getSetId() + ":object:" + fact.getConceptName()).getBytes()); 
					if (!nonSnomedTerms.contains(draftFactTargetUUID))
					{
						nonSnomedTerms.add(draftFactTargetUUID);
						EConcept newObject = conceptUtility_.createConcept(draftFactTargetUUID,
								fact.getConceptName(), System.currentTimeMillis());
						conceptUtility_.addRelationship(newObject, nonSnomed, null);
						draftFactTargetUUID = newObject.getPrimordialUuid();
						storeConcept(newObject);						
					}
				}
				else
				{
					draftFactTargetUUID = Type3UuidFactory.fromSNOMED(fact.getConceptCode());
				}

				TkRefsetCidCidCidMember triple = conceptUtility_.addCIDTriple(concept, concept.getPrimordialUuid(), draftFactRoleUUID, 
						draftFactTargetUUID, StaticDataType.DRAFT_FACT_TRIPLE.getUuid());

				conceptUtility_.addAnnotation(triple, fact.getConceptName(), StaticDataType.DRAFT_FACT_SNOMED_CONCEPT_NAME.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getConceptCode(), StaticDataType.DRAFT_FACT_SNOMED_CONCEPT_CODE.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getSecName(), StaticDataType.DRAFT_FACT_SEC_NAME.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getSentence(), StaticDataType.DRAFT_FACT_SENTENCE.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getRowId(), StaticDataType.DRAFT_FACT_UNIQUE_ID.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getCurationState(), StaticDataType.DRAFT_FACT_CURATION_STATE.getUuid());
				conceptUtility_.addAnnotation(triple, fact.getComment(), StaticDataType.DRAFT_FACT_COMMENT.getUuid());
				
				if (fact.getCurationState() != null)
				{
					conceptUtility_.addAnnotation(triple, fact.getSentence(), StaticDataType.CURATION_STATE.getUuid());
				}
				
				if (fact.getComment() != null)
				{
					conceptUtility_.addAnnotation(triple, fact.getSentence(), StaticDataType.COMMENT.getUuid());
				}
			}
		}
		
		for(ConceptData c : conMap.values())
		{
			storeConcept(c.con);			
		}
	}
	
	private DynamicDataType getNDAType(String type) throws Exception
	{
		DynamicDataType ddt = dynamicDataTypes_.get(type);
		if (ddt == null)
		{
			UUID typeId = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types:ndaTypes:" + type).getBytes());
			writeAuxEConcept(typeId, type, "", ndaTypeRoot_);
			ddt = new DynamicDataType(type, typeId);
			dynamicDataTypes_.put(type, ddt);
		}
		return ddt;
	}
	
	private DynamicDataType getDraftFactType(String type) throws Exception
	{
		DynamicDataType ddt = dynamicDataTypes_.get(type);
		if (ddt == null)
		{
			UUID typeId = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types:draftFacts:" + type).getBytes());
			writeAuxEConcept(typeId, type, "", draftFactsRoot_);
			ddt = new DynamicDataType(type, typeId);
			dynamicDataTypes_.put(type, ddt);
		}
		return ddt;
	}

	/**
	 * Utility method to build and store a metadata concept.  description is optional.
	 */
	private void writeAuxEConcept(UUID primordial, String name, String description, UUID relParentPrimordial) throws Exception
	{
		EConcept concept = conceptUtility_.createConcept(primordial, name, System.currentTimeMillis());
		conceptUtility_.addRelationship(concept, relParentPrimordial, null);
		if (description != null && description.length() > 0)
		{
			conceptUtility_.addDescription(concept, description, ArchitectonicAuxiliary.Concept.TEXT_DEFINITION_TYPE.getPrimoridalUid());
		}
		storeConcept(concept);
		metadataConceptCounter_++;
	}

	/**
	 * Write an EConcept out to the jbin file. Updates counters, prints status tics.
	 */
	private void storeConcept(EConcept concept) throws IOException
	{
		concept.writeExternal(dos_);
		conceptCounter_++;

		if (conceptCounter_ % 10 == 0)
		{
			System.out.print(".");
		}
		if (conceptCounter_ % 500 == 0)
		{
			System.out.println("");
		}
		if ((conceptCounter_ % 1000) == 0)
		{
			System.out.println("Processed: " + conceptCounter_ + " - just completed " + concept.getDescriptions().get(0).getText());
		}
	}

	public File[] getFacts() {
		return facts;
	}

	public void setFacts(File[] facts) {
		this.facts = facts;
	}

	public File getZips() {
		return zips;
	}

	public void setZips(File zips) {
		this.zips = zips;
	}

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public boolean isFilterNda() {
		return filterNda;
	}

	public void setFilterNda(boolean filterNda) {
		this.filterNda = filterNda;
	}


	
	
}
