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
import gov.va.akcds.splMojo.model.Drug;
import gov.va.akcds.splMojo.model.SimpleDraftFact;
import gov.va.akcds.splMojo.model.SimpleDraftFactSource;
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.util.id.Type3UuidFactory;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.refset.cidcidcid.TkRefsetCidCidCidMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;

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
	 * Location of the output file.
	 * 
	 * @parameter
	 * @required
	 */
	private String outputFileName;
	
	/**
	 * Location of the data.
	 * 
	 * @parameter
	 * @required
	 */
	private File[] facts;
	
	/**
	 * The NDA filter flag.
	 * 
	 * @parameter
	 * @required
	 */
	private boolean filterNda;
	
	
	/**
	 * Location of the SPL source data.
	 * 
	 * @parameter
	 * @required
	 */
	private File splZipFile;

	private EConceptUtility conceptUtility_ = new EConceptUtility(uuidRoot_);

	private DraftFacts draftFacts;
	
	private Hashtable<String, DynamicDataType> dynamicDataTypes_ = new Hashtable<String, DynamicDataType>();
	private Hashtable<Integer, UUID> letterRoots_ = new Hashtable<Integer, UUID>();
	private Hashtable<Integer, UUID> nsLetterRoots_ = new Hashtable<Integer, UUID>();
	private UUID ndaTypeRoot_, draftFactsRoot_;

	private DataOutputStream dos_;
	private long metadataConceptCounter_ = 0;
	private long conceptCounter_ = 0;
	private long xmlFileCnt_ = 0;
	private ArrayList<String> dropForNoFacts_ = new ArrayList<String>();
	private ArrayList<String> dropForNoNDAs_ = new ArrayList<String>();
	private int dropCurationDataForConflict_ = 0;
	
	private boolean createLetterRoots_ = true;  //switch this to false to generate a flat structure under the spl root concept
	
	private Hashtable<String, Drug> splDrugConcepts_ = new Hashtable<String, Drug>();
	private Hashtable<UUID, EConcept> splSetIdConcepts_ = new Hashtable<UUID, EConcept>();
	
	private Set<UUID> nonSnomedTerms_ = new HashSet<UUID>();
	private UUID rootConceptUUID_, nonSnomedRootConceptUUID_;
	

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
			System.out.println("Writing output to " + getOutputDirectory());
			if (getOutputDirectory().exists() == false)
			{
				getOutputDirectory().mkdirs();
			}

			// jbin (output) file
			File jbinFile = new File(getOutputDirectory(), getOutputFileName());
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(jbinFile)));

			createMetaData();

			// Create the root concept (named SPL)
			EConcept rootConcept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root").getBytes()), "SPL",
					System.currentTimeMillis());
			conceptUtility_.addDescription(rootConcept, "1.0",  StaticDataType.VERSION.getUuid());
			storeConcept(rootConcept);
			rootConceptUUID_ = rootConcept.getPrimordialUuid();
			metadataConceptCounter_++;			
			
			// Create the root concept (named non-snomed)
			EConcept nonSnoRootConcept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root:non-snomed").getBytes()), "Non-SNOMED CT", System.currentTimeMillis());
			conceptUtility_.addDescription(nonSnoRootConcept, "1.0",  StaticDataType.VERSION.getUuid());
			storeConcept(nonSnoRootConcept);
			nonSnomedRootConceptUUID_ = nonSnoRootConcept.getPrimordialUuid();
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
					
					concept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root:non-snomed" + s).getBytes()), s, System.currentTimeMillis());
					nsLetterRoots_.put(i, concept.getPrimordialUuid());
					conceptUtility_.addRelationship(concept, nonSnoRootConcept.getPrimordialUuid(), null);
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
			File dataFile= getSplZipFile();
			
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
						loadIntoModel(spl);
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
			System.out.println("Data loaded, filtered and normalized.  Found " + splDrugConcepts_.size() + " unique drugs.  Converting results to workbench format");
			
			for (Drug d : splDrugConcepts_.values())
			{
				UUID key = UUID.nameUUIDFromBytes((uuidRoot_ + ":" + d.drugName).getBytes());

				EConcept concept = conceptUtility_.createConcept(key, d.drugName, System.currentTimeMillis());

				// Find the right letter parent to to place it under.
				UUID parentUUID = null;
				if (createLetterRoots_)
				{
					for (int i = 0; i < d.drugName.length(); i++)
					{
						if (parentUUID != null)
						{
							break;
						}
						parentUUID = letterRoots_.get(d.drugName.codePointAt(i));
					}

				}
				if (parentUUID == null)
				{
					// Still null? No letters in the name?
					parentUUID = rootConceptUUID_;
				}
				conceptUtility_.addRelationship(concept, parentUUID, null);

				//Link to the SPL Set ID item
				for (UUID setId : d.setIds)
				{
					conceptUtility_.addRelationship(splSetIdConcepts_.get(setId), concept.getPrimordialUuid(), null);
				}
				
				//Add add the draft facts
				for (SimpleDraftFact sdf : d.draftFacts.values())
				{
					TkRefsetCidCidCidMember triple = conceptUtility_.addCIDTriple(concept, 
							concept.getPrimordialUuid(), 
							getDraftFactType(sdf.relName).getIdentifier(), 
							sdf.targetCodeUUID, StaticDataType.DRAFT_FACT_TRIPLE.getUuid());
					
					conceptUtility_.addAnnotation(triple, sdf.targetCodeName, StaticDataType.DRAFT_FACT_SNOMED_CONCEPT_NAME.getUuid());
					conceptUtility_.addAnnotation(triple, sdf.targetCode, StaticDataType.DRAFT_FACT_SNOMED_CONCEPT_CODE.getUuid());
					
					if (sdf.curationState != null && !sdf.curationState.equals("-CONFLICT-"))
					{
						conceptUtility_.addAnnotation(triple, sdf.curationState, StaticDataType.DRAFT_FACT_CURATION_STATE.getUuid());
					}
					
					//Need to build a new, unique ID for this conglomerate draft fact
					StringBuilder superDraftFactId = new StringBuilder();
					
					for (SimpleDraftFactSource sdfs : sdf.sources)
					{
						superDraftFactId.append(sdfs.rowId);
						superDraftFactId.append("-");
						
						TkRefsetStrMember annotation = conceptUtility_.addAnnotation(triple, sdfs.rowId, StaticDataType.DRAFT_FACT_UNIQUE_ID.getUuid());
						conceptUtility_.addAnnotation(annotation, sdfs.setId, StaticDataType.DRAFT_FACT_SET_ID.getUuid());
						conceptUtility_.addAnnotation(annotation, sdfs.sectionName, StaticDataType.DRAFT_FACT_SEC_NAME.getUuid());
						conceptUtility_.addAnnotation(annotation, sdfs.sentenceContext, StaticDataType.DRAFT_FACT_SENTENCE.getUuid());
						if (sdfs.ndc != null && !sdfs.ndc.equals("-"))
						{
							conceptUtility_.addAnnotation(annotation, sdfs.ndc, StaticDataType.DRAFT_FACT_DRUG_CODE.getUuid());
						}
						if (sdfs.curationComment != null)
						{
							conceptUtility_.addAnnotation(annotation, sdfs.curationComment, StaticDataType.DRAFT_FACT_COMMENT.getUuid());
						}
					}
					
					if (superDraftFactId.length() > 0)
					{
						superDraftFactId.setLength(superDraftFactId.length() - 1);
					}
					
					conceptUtility_.addAnnotation(triple, superDraftFactId.toString(), StaticDataType.SUPER_DRAFT_FACT_UNIQUE_ID.getUuid());
				}
				
				storeConcept(concept);
			}
			
			//Finally, store the setId concepts
			for (EConcept concept : splSetIdConcepts_.values())
			{
				storeConcept(concept);
			}

			System.out.println();
			
			dos_.flush();
			dos_.close();						
			
			// Summarize
			System.out.println("TOTAL SPL FILES:   " + xmlFileCnt_);
			System.out.println("TOTAL concepts created: " + conceptCounter_);
			System.out.println("Metadata concepts created: " + metadataConceptCounter_);
			System.out.println("Created " + nonSnomedTerms_.size() + " non-snomed concepts");
			System.out.println("SPL Drug Concepts created: " + splDrugConcepts_.size());
			System.out.println("SPL Set ID concepts created: " + (conceptCounter_ - metadataConceptCounter_ - nonSnomedTerms_.size() - splDrugConcepts_.size()));
			System.out.println("Ignored " + dropForNoFacts_.size() + " files for not having any draft facts");
			System.out.println("Ignored " + dropForNoNDAs_.size() + " files for not having any NDAs");		
			System.out.println("Dropped " + dropCurationDataForConflict_ + " draft fact curation data for conflicts");
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
	
	private void loadIntoModel(Spl spl) throws Exception
	{
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
		
		if (splDraftFacts != null)
		{
			//Create the spl Set id object which will be attached to one (or more) drug concepts
			UUID setIdUUID = loadSetId(spl);

			
			for (int i = 0; i < splDraftFacts.size(); i++)
			{
				DraftFact fact = splDraftFacts.get(i);

				// default version to that of current doc, for npl data as we don't know what it is
				if (fact.getSplVersion().equals("-"))
				{
					fact.setSplVersion(spl.getVersion());
				}
				
				// check the fact version matches that of this doc
				if (!fact.getSplVersion().equals(spl.getVersion()))
				{
					continue;
				}
				
				//Ok, we want to load this one.  See if we have already started it.
				String drugName = fact.getDrugName().toUpperCase();
				
				Drug drug = splDrugConcepts_.get(drugName);
				
				if (drug == null)
				{
					drug = new Drug(drugName);
					splDrugConcepts_.put(drugName, drug);
				}
				
				drug.setIds.add(setIdUUID);
				
				//Now, set up this draft fact
				SimpleDraftFact newSdf = new SimpleDraftFact(drugName, fact.getRoleName(), (fact.getConceptCode().equals("-") ? fact.getConceptName() : fact.getConceptCode()));
				SimpleDraftFact existingSdf = drug.draftFacts.get(newSdf.getUniqueKey());
				if (existingSdf == null)
				{
					//Use our new one... finish populating the standard data...
					drug.draftFacts.put(newSdf.getUniqueKey(), newSdf);
					existingSdf = newSdf;
					existingSdf.targetCodeName = fact.getConceptName();
					
					//Need to do some work to generate the target UUID
					
					//This is an oddity discovered later in the draft facts... which wasn't documented.  Not sure what the correct solution is, 
					//but will do this for now....								
					if (fact.getConceptCode().equals("1"))
					{
						existingSdf.targetCodeUUID = StaticDataType.DRAFT_FACT_TRUE.getUuid();
					}
					else if (fact.getConceptCode().equals("0"))
					{
						existingSdf.targetCodeUUID = StaticDataType.DRAFT_FACT_FALSE.getUuid();
					}				
					else if (fact.getConceptCode().equals("-"))
					{
						// if the code is not set then we have a non-snomed concept
						existingSdf.targetCodeUUID = UUID.nameUUIDFromBytes((uuidRoot_ + ":root:non-snomed:" + fact.getConceptName()).getBytes());
						createMissingConceptIfNecessary(existingSdf.targetCodeUUID, fact.getConceptName());
					}
					else // get the snomed concept
					{
						existingSdf.targetCodeUUID = Type3UuidFactory.fromSNOMED(fact.getConceptCode());
					}
				}
				
				//Add on the unique draft fact data for this instance of the draft fact
				SimpleDraftFactSource sdfs = new SimpleDraftFactSource(fact.getRowId(), fact.getSplSetId(), fact.getSecName(), fact.getSentence(), fact.getDrugCode());
				if (fact.getComment() != null && !fact.getComment().equals("-"))
				{
					sdfs.curationComment = fact.getComment();
				}
				existingSdf.sources.add(sdfs);
	
				if (existingSdf.curationState == null)
				{
					if (fact.getCurationState() != null && !fact.getCurationState().equals("-"))
					{
						existingSdf.curationState = fact.getCurationState();
					}
				}
				else
				{
					//sanity check - they should be identical, yes?
					if (fact.getCurationState() != null)
					{
						if (!fact.getCurationState().equals(existingSdf.curationState))
						{
							if (!existingSdf.curationState.equals("-CONFLICT-"))
							{
								System.err.println("Different curations states listed for same fact: " + existingSdf.getUniqueKey()  + " " + existingSdf.curationState + " " + fact.getCurationState());
								dropCurationDataForConflict_++;
								existingSdf.curationState = "-CONFLICT-";
							}
						}
					}
				}
			}
		}
	}
	
	private UUID loadSetId(Spl spl) throws Exception
	{
		UUID setIdUUID = UUID.nameUUIDFromBytes((uuidRoot_ + ":root:setIds:" + spl.getSetId()).getBytes());
		EConcept concept = conceptUtility_.createConcept(setIdUUID, "SPL Source", System.currentTimeMillis());
		
		conceptUtility_.addAnnotation(concept, spl.getSetId(), StaticDataType.SPL_SET_ID.getUuid());
		for (NDA nda : spl.getUniqueNDAs())
		{
			DynamicDataType ddt = getNDAType(nda.getType());
			conceptUtility_.addAnnotation(concept, nda.getValue(), ddt.getIdentifier());
		}
		conceptUtility_.addAnnotation(concept, spl.getXMLFileAsString(), StaticDataType.SPL_XML_TEXT.getUuid());
		conceptUtility_.addAnnotation(concept, spl.getVersion(), StaticDataType.SPL_VERSION.getUuid());

		ArrayList<ZipFileContent> media = spl.getSupportingFiles();
		for (ZipFileContent zfc : media)
		{
			String fileName = zfc.getName();
			byte[] data = zfc.getFileBytes();
			int splitPos = fileName.lastIndexOf('.');
			String extension = ((splitPos + 1 <= fileName.length() ? fileName.substring(splitPos + 1) : ""));
			conceptUtility_.addMedia(concept, StaticDataType.SPL_IMAGE.getUuid(), data, extension, fileName);
		}
		
		//Can't store these yet, because we need to add all the tree links first (which we don't know yet)
		splSetIdConcepts_.put(setIdUUID, concept);
		return setIdUUID;
	}
	
	private void createMissingConceptIfNecessary(UUID uuid, String conceptName) throws IOException
	{
		if (!nonSnomedTerms_.contains(uuid))
		{
			nonSnomedTerms_.add(uuid);
			EConcept newObject = conceptUtility_.createConcept(uuid, conceptName, System.currentTimeMillis());
			
			
			UUID parentUUID = null;
			if (createLetterRoots_)
			{
				for (int pos = 0; pos < conceptName.length(); pos++)
				{
					if (parentUUID != null)
					{
						break;
					}
					parentUUID = nsLetterRoots_.get(conceptName.toUpperCase().codePointAt(pos));
				}
			}
			
			if (parentUUID == null)
			{
				parentUUID = nonSnomedRootConceptUUID_;
			}
			conceptUtility_.addRelationship(newObject, parentUUID, null);	
			storeConcept(newObject);
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

	public File getSplZipFile() {
		return splZipFile;
	}

	public void setSplZipFile(File zip) {
		this.splZipFile = zip;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
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


	public static void main(String[] args) throws Exception
	{
		SplMojo mojo = new SplMojo();
		//new File("../splData/data/splDraftFacts.txt.zip")
		mojo.facts = new File[] {new File("../splData/data/bwDraftFacts.txt.zip")};
		mojo.splZipFile = new File("../splData/data/splSrcFiles.zip");
		mojo.outputFileName = "splData.jbin";
		mojo.filterNda = true;
		mojo.outputDirectory = new File("../splData/target");
		
		mojo.execute();
	}
	
	
}
