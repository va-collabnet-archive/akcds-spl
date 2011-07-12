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
import gov.va.akcds.spl.SplDataHolder;
import gov.va.akcds.splMojo.dataTypes.DynamicDataType;
import gov.va.akcds.splMojo.dataTypes.StaticDataType;
import gov.va.akcds.splMojo.model.Drug;
import gov.va.akcds.splMojo.model.SimpleDraftFact;
import gov.va.akcds.splMojo.model.SimpleDraftFactSource;
import gov.va.akcds.util.ConsoleUtil;
import gov.va.akcds.util.EConceptUtility;
import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.ihtsdo.tk.dto.concept.component.attribute.TkConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.cidcidcid.TkRefsetCidCidCidMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;

import com.apelon.splRxNormMap.data.DataMaps;
import com.apelon.splRxNormMap.data.NdcAsKey;
import com.apelon.splRxNormMap.data.NdcAsKeyData;
import com.apelon.splRxNormMap.data.SplAsKey;
import com.apelon.splRxNormMap.data.SplAsKeyData;
import com.apelon.splRxNormMap.data.ViewDataFile;

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
	 * The missing SPL filter flag.
	 * 
	 * @parameter
	 * @required
	 */
	private boolean loadFactsWithMissingSPL;
	
	
	/**
	 * Location of the SPL source data.
	 * 
	 * @parameter
	 * @required
	 */
	private File splZipFilesFolder;
	
	/**
	 * Location of the RXNorm mapping data.
	 * 
	 * @parameter
	 * @required
	 */
	private File rxNormMapFile;

	private EConceptUtility conceptUtility_ = new EConceptUtility(uuidRoot_);

	private DraftFacts draftFacts;
	
	private Hashtable<String, DynamicDataType> dynamicDataTypes_ = new Hashtable<String, DynamicDataType>();
	private Hashtable<Integer, UUID> letterRoots_ = new Hashtable<Integer, UUID>();
	private Hashtable<Integer, UUID> nsLetterRoots_ = new Hashtable<Integer, UUID>();
	private UUID ndaTypeRoot_, draftFactsRoot_;

	private DataOutputStream dos_;
	private long metadataConceptCounter_ = 0;
	private long conceptCounter_ = 0;
	private long skipDraftFactForWrongVersion_ = 0;
	private long duplicateDraftFactMerged_ = 0;
	private long uniqueDraftFactCount_ = 0;
	private long accept_ = 0;
	private long reject_ = 0;
	private long new_ = 0;
	private long droppedFactsForNoNDAs_ = 0;
	private long droppedFactsForMissingSpl_ = 0;
	private int flagCurationDataForConflict_ = 0;
	private HashSet<String> uniqueTargetConcepts_ = new HashSet<String>();
	private ArrayList<String> mismatchedCurationStateErrors_ = new ArrayList<String>();
	
	//An uber mapping of unique SCT codes (real codes only) to (a set of) unique draft facts to the set of unique SPL Set IDs
	private Hashtable<String, Hashtable<String, HashSet<String>>> sctFactLabelCounts = new Hashtable<String, Hashtable<String, HashSet<String>>>();
	
	//A mapping of splSetIds -> drug names
	Hashtable <String, String> reverseDrugMap = new Hashtable<String, String>();
	
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
			ConsoleUtil.println("TPS report completed 04/05/2011.");
			ConsoleUtil.println("Starting creation of .jbin file for Structured Product Labels (SPLs)");
			ConsoleUtil.println(new Date().toString());

			// output directory
			ConsoleUtil.println("Writing output to " + getOutputDirectory());
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
			
			ConsoleUtil.println("");
			ConsoleUtil.println("Created " + metadataConceptCounter_ + " initial metadata concepts");
			
			//Index / prepare the SPL zip files
			ConsoleUtil.println("Preparing SPL source files:");
			SplDataHolder sdh = new SplDataHolder(splZipFilesFolder.listFiles());
			
			// Start iterating the draft facts
			ConsoleUtil.println("Processing draft facts:");
			draftFacts = new DraftFacts(getFacts());

		
			ConsoleUtil.println(new Date().toString());
			// process the zip of zips
			

			while (draftFacts.hasMoreElements())
			{
				ArrayList<DraftFact> currentDraftFacts = draftFacts.nextElement();
				//Each draftFact returned in a batch will have the same set id.
				//Each draftFact returned should have the same setId version.  If not, we catch the error later.
				
				if (currentDraftFacts.size() > 0)
				{
					Spl spl = sdh.getSpl(currentDraftFacts.get(0).getSplSetId(), currentDraftFacts.get(0).getSplVersion());
					loadIntoModel(spl, currentDraftFacts);
				}
				else
				{
					ConsoleUtil.printErrorln("Empty Draft fact set?");
				}
			}
			
			ConsoleUtil.println("");
			ConsoleUtil.println("Data loaded, filtered and normalized.  Found " + splDrugConcepts_.size() + " unique drugs.  Searching for RxNorm VUIDs");
			
			addVUIDMappings();
			
			ConsoleUtil.println("Converting results to workbench format");
			
			
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
				for (UUID setId : d.setIdUUIDs)
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
					
					uniqueTargetConcepts_.add(sdf.targetCodeUUID.toString());  //Just for counting purposes.
					
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
				
				//Add the VUID identifiers that we found in in the rxNorm mapping
				for (String vuid : d.rxNormVuids)
				{
					conceptUtility_.addAnnotation(concept, vuid, StaticDataType.RXNorm_VUID.getUuid());
				}
				
				storeConcept(concept);
			}
			
			//Finally, store the setId concepts
			for (EConcept concept : splSetIdConcepts_.values())
			{
				storeConcept(concept);
			}

			ConsoleUtil.println("");
			
			dos_.flush();
			dos_.close();						
			
			// Summarize
			ConsoleUtil.println("Total workbench concepts created: " + conceptCounter_);
			ConsoleUtil.println("Metadata concepts created: " + metadataConceptCounter_);
			ConsoleUtil.println("Created " + nonSnomedTerms_.size() + " non-snomed concepts");
			ConsoleUtil.println("SPL Drug Concepts created: " + splDrugConcepts_.size());
			ConsoleUtil.println("Unique set IDs processed / SPL Set ID concepts created: " + splSetIdConcepts_.size());
			if ((conceptCounter_ - metadataConceptCounter_ - nonSnomedTerms_.size() - splDrugConcepts_.size()) != splSetIdConcepts_.size())
			{
				ConsoleUtil.printErrorln("Math error in counters!");
			}
			ConsoleUtil.println("Processed " + draftFacts.getTotalDraftFactCount() + " draft facts");
			ConsoleUtil.println("Merged " + duplicateDraftFactMerged_ + " duplicate draft facts onto an existing draft fact.");
			ConsoleUtil.println("Loaded " + uniqueDraftFactCount_ + " draft facts");
			if (isFilterNda())
			{
				ConsoleUtil.println("Ignored " + droppedFactsForNoNDAs_ + " facts because the corresponding SPL did not have any NDAs");
			}
			else
			{
				ConsoleUtil.println("Would have ignored " + droppedFactsForNoNDAs_ + " facts because the corresponding SPL did not have any NDAs, however the nda filter is disabled.");
			}
			ConsoleUtil.println("Ignored " + skipDraftFactForWrongVersion_ + " draft facts for not matching the draft fact version number of a previously loaded draft fact on the same set id");
			if (loadFactsWithMissingSPL)
			{
				ConsoleUtil.println("Ignored " + droppedFactsForMissingSpl_+ " draft facts for not having an spl file available");
			}
			ConsoleUtil.println("Facts Accepted: " + accept_);
			ConsoleUtil.println("Facts Rejected: " + reject_);
			ConsoleUtil.println("Facts NEW?: " + new_);
			ConsoleUtil.println("Facts flagged for review: " + flagCurationDataForConflict_);		
			ConsoleUtil.println("Unique target concepts: " + uniqueTargetConcepts_.size());	
			
			ConsoleUtil.println("Unique real SCT concepts " + sctFactLabelCounts.size());
			ConsoleUtil.println("Also see: " + new File(getOutputDirectory(), "stats.csv") + " for more stats");
			
			FileWriter statsFile = new FileWriter(new File(getOutputDirectory(), "stats.csv"));
			statsFile.write("code,unique draft facts,unique label count,unique drug count\r\n");
			
			for (Map.Entry<String, Hashtable<String, HashSet<String>>> x : sctFactLabelCounts.entrySet())
			{
				HashSet<String> uniqueDrugs = new HashSet<String>();
				int labelTotal = 0;
				Hashtable<String, HashSet<String>> factAndLabel = x.getValue();
				for (HashSet<String> i : factAndLabel.values())
				{
					labelTotal = labelTotal + i.size();
					for (String setId : i)
					{
						String drug = reverseDrugMap.get(setId);
						if (drug != null)
						{
							uniqueDrugs.add(drug);
						}
					}
				}
				
				//Find all of the unique drugs that any involved setId points to.
				
				statsFile.write(x.getKey() + "," + factAndLabel.size() + "," + labelTotal + "," + uniqueDrugs.size() + "\r\n");
			}
			statsFile.close();
			
			if (mismatchedCurationStateErrors_.size() > 0)
			{
				ConsoleUtil.println("Curation state mismatches on " + mismatchedCurationStateErrors_.size() + " draft facts.  See: " 
						+ new File(getOutputDirectory(), "curationsErrors.csv") + ".");
				
				FileWriter curationErrorFile = new FileWriter(new File(getOutputDirectory(), "curationsErrors.csv"));
				curationErrorFile.write("unique key,previously found state,newly found state\r\n");
				for (String s : mismatchedCurationStateErrors_)
				{
					curationErrorFile.write(s + "\r\n");
				}
				curationErrorFile.close();
			}
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
	
	private void loadIntoModel(Spl spl, ArrayList<DraftFact> splDraftFacts) throws Exception
	{
		if (spl == null)
		{
			ConsoleUtil.printErrorln("Missing SPL document for set id: " + splDraftFacts.get(0).getSplSetId());
			if (loadFactsWithMissingSPL)
			{
				//Make up a "missing" placeholder.
				spl = new Spl(splDraftFacts.get(0).getSplSetId());
			}
			else
			{
				droppedFactsForMissingSpl_ += splDraftFacts.size();
				return;
			}
		}
		if (!spl.hasAtLeastOneNDA())
		{
			// if there are no facts don't add the spl (it complicates things) (but in the revised loader, this wont happen, 
			//since we process from the draft facts file.
			// So just drop anything with no nda values
			droppedFactsForNoNDAs_ += splDraftFacts.size();
			if (isFilterNda())
			{
				ConsoleUtil.printErrorln("Spl " + spl.getZipFileName() + " has no NDAs.  Skipping all draft facts for this set id: " + spl.getSetId());
				return;
			}
		}

		//Create the spl Set id object which will be attached to one (or more) drug concepts
		UUID setIdUUID = null;
		String splVersion = null;

		
		for (int i = 0; i < splDraftFacts.size(); i++)
		{
			DraftFact fact = splDraftFacts.get(i);

			// default version to that of current doc, for npl data as we don't know what it is
			if (fact.getSplVersion().equals("-"))
			{
				fact.setSplVersion(spl.getVersion());
			}
			
			//This will ensure that all setIds in this batch have the same version
			if (splVersion == null)
			{
				splVersion = fact.getSplVersion();
			}
			if (!fact.getSplVersion().equals(splVersion))
			{
				skipDraftFactForWrongVersion_++;
				continue;
			}
			
			//This will ensure that we haven't already loaded this setId with a different version
			if (setIdUUID == null)
			{
				setIdUUID = loadSetId(spl);
			}
			if (setIdUUID == null)
			{
				//skip this entire set.
				skipDraftFactForWrongVersion_ += splDraftFacts.size();
				ConsoleUtil.printErrorln("Skipping draft facts for set id " + spl.getSetId() 
						+ " because we have already loaded draft facts for this set id with a different version number.  Skipped version number: " + spl.getVersion());
				break;
			}
			
						
			//Ok, we want to load this one.  See if we have already started it.
			String drugName = fact.getDrugName().toUpperCase();
			
			Drug drug = splDrugConcepts_.get(drugName);
			
			if (drug == null)
			{
				drug = new Drug(drugName);
				splDrugConcepts_.put(drugName, drug);
			}
			
			//Also load this mapping hashtable (used for stats)
			reverseDrugMap.put(fact.getSplSetId(), drugName);
			
			drug.setIdUUIDs.add(setIdUUID);
			drug.setIds.add(fact.getSplSetId());
			
			//Now, set up this draft fact
			SimpleDraftFact newSdf = new SimpleDraftFact(drugName, fact.getRoleName(), (fact.getConceptCode().equals("-") ? fact.getConceptName() : fact.getConceptCode()));
			SimpleDraftFact existingSdf = drug.draftFacts.get(newSdf.getUniqueKey());
			if (existingSdf == null)
			{
				uniqueDraftFactCount_++;
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
			else
			{
				duplicateDraftFactMerged_++;
			}
			
			//Store some other stats....
			
			if (fact.getConceptCode().length() > 1)
			{
				//A real SCT code.
				Hashtable<String, HashSet<String>> stats = sctFactLabelCounts.get(fact.getConceptCode());
				if (stats == null)
				{
					stats = new Hashtable<String, HashSet<String>>();
					sctFactLabelCounts.put(fact.getConceptCode(), stats);
				}
				HashSet<String> labels = stats.get(existingSdf.getUniqueKey());
				if (labels == null)
				{
					labels = new HashSet<String>();
					stats.put(existingSdf.getUniqueKey(), labels);
				}
				labels.add(fact.getSplSetId());
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
					if (fact.getCurationState().equalsIgnoreCase("Accept"))
					{
						accept_++;
					}
					else if (fact.getCurationState().equalsIgnoreCase("Reject"))
					{
						reject_++;
					}
					else if (fact.getCurationState().equalsIgnoreCase("flag"))
					{
						flagCurationDataForConflict_++;
					}
					else if (fact.getCurationState().equalsIgnoreCase("NEW"))
					{
						//TODO deal with this how?
						new_++;
					}
					else
					{
						ConsoleUtil.printErrorln("Unexpected curation state: '" + fact.getCurationState() + "'");
					}
				}
			}
			else
			{
				//sanity check - they should be identical, yes?
				if (fact.getCurationState() != null)
				{
					if (!fact.getCurationState().equalsIgnoreCase(existingSdf.curationState))
					{
						if (!existingSdf.curationState.equalsIgnoreCase("flag"))
						{
							mismatchedCurationStateErrors_.add(existingSdf.getUniqueKey()  + "," + existingSdf.curationState + "," + fact.getCurationState());
							if (existingSdf.curationState.equalsIgnoreCase("Accept"))
							{
								accept_--;
							}
							else if (existingSdf.curationState.equalsIgnoreCase("Reject"))
							{
								reject_--;
							}
							else if (existingSdf.curationState.equalsIgnoreCase("New"))
							{
								new_--;
							}
							else
							{
								//Unexpected state, but we already wrote an error about this when we put it in.
							}
							flagCurationDataForConflict_++;
							existingSdf.curationState = "flag";
						}
					}
				}
			}
		}
	}
	
	private UUID loadSetId(Spl spl) throws Exception
	{
		UUID setIdUUID = UUID.nameUUIDFromBytes((uuidRoot_ + ":root:setIds:" + spl.getSetId()).getBytes());
		
		EConcept concept = splSetIdConcepts_.get(setIdUUID);
		if (concept != null)
		{
			//It has to have the same version.  If it has the same version, nothing to do here.
			//If it doesn't, we need to report back that this draft fact should be skipped, because it came from the wrong spl document.
			TkConceptAttributes attribs = concept.getConceptAttributes();
			String version = "";
			for (TkRefsetAbstractMember<?> x : attribs.getAnnotations())
			{
				if (x.getRefsetUuid().equals(StaticDataType.SPL_VERSION))
				{
					version = ((TkRefsetStrMember)x).getStrValue();
					break;
				}
			}
			
			if (spl.getVersion().equals(version))
			{
				return setIdUUID;
			}
			else
			{
				return null;
			}
		}
		
		concept = conceptUtility_.createConcept(setIdUUID, "SPL Source", System.currentTimeMillis());
		
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
	
	private void addVUIDMappings() throws FileNotFoundException, IOException, ClassNotFoundException
	{
		ConsoleUtil.println("Loading RXNorm Map Data");
		DataMaps rxNormMaps = ViewDataFile.readData(rxNormMapFile);
		ConsoleUtil.println(new Date(rxNormMaps.getBuildDate()) + "");
		ConsoleUtil.println(rxNormMaps.getSourceDescription());
		ConsoleUtil.println("NDC as key: " + rxNormMaps.getNdcAsKey().size());
		ConsoleUtil.println("SPL as key: " + rxNormMaps.getSplAsKey().size());
		
		File f = new File(getOutputDirectory(), "mappingStats.csv");
		ConsoleUtil.println("Also see: " + f + " for more stats");
		
		FileWriter statsFile = new FileWriter(f);
		statsFile.write("Drug Name,SPL -> VUID,NDC->VUID,Unique VUIDs\r\n");
		
		//We only have 8 or 9 digits of the drug code.  RXNorm has more.  Create new maps from rxNorm that have 8 and 9 digits, respectively.
		Hashtable<String, NdcAsKey> rxNormDrugCodeEightMatch = new Hashtable<String, NdcAsKey>();
		Hashtable<String, NdcAsKey> rxNormDrugCodeNineMatch = new Hashtable<String, NdcAsKey>();
		
		for (String s : rxNormMaps.getNdcAsKey().keySet())
		{
			if (s.length() >= 8)
			{
				rxNormDrugCodeEightMatch.put(s.substring(0, 8), rxNormMaps.getNdcAsKey().get(s));
			}
			if (s.length() >= 8)
			{
				rxNormDrugCodeNineMatch.put(s.substring(0, 9), rxNormMaps.getNdcAsKey().get(s));
			}
		}
		
		for (Drug d : splDrugConcepts_.values())
		{
			int foundBySpl = 0;
			int foundByNDC = 0;
			for (String setId : d.setIds)
			{
				SplAsKey sak = rxNormMaps.getSplAsKey().get(setId);
				if (sak != null)
				{
					for (SplAsKeyData sakd : sak.getCodes())
					{
						d.rxNormVuids.addAll(sakd.getVuid());
						foundBySpl += sakd.getVuid().size();
					}
				}
			}
			
			for (SimpleDraftFact sdf : d.draftFacts.values())
			{
				for (SimpleDraftFactSource sdfs : sdf.sources)
				{
					NdcAsKey nak = rxNormDrugCodeEightMatch.get(sdfs.ndc.toUpperCase());
					if (nak == null)
					{
						nak = rxNormDrugCodeNineMatch.get(sdfs.ndc.toUpperCase());
					}
					if (nak != null)
					{
						for (NdcAsKeyData nakd : nak.getCodes())
						{
							d.rxNormVuids.addAll(nakd.getVuid());
							foundByNDC += nakd.getVuid().size();
						}
					}
				}
			}
			statsFile.write(d.drugName + "," + foundBySpl + "," + foundByNDC + "," + d.rxNormVuids.size() + "\r\n");
		}
		statsFile.close();
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
			ConsoleUtil.showProgress();
		}
	}
	
	

	public File[] getFacts() {
		return facts;
	}

	public void setFacts(File[] facts) {
		this.facts = facts;
	}

	public File getSplZipFilesFolder() {
		return splZipFilesFolder;
	}

	public void setSplZipFilesFolder(File zip) {
		this.splZipFilesFolder = zip;
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
	
	public boolean isLoadFactsWithMissingSpl() {
		return loadFactsWithMissingSPL;
	}

	public void setLoadFactsWithMissingSPL(boolean loadFactsWithMissingSPL) {
		this.loadFactsWithMissingSPL = loadFactsWithMissingSPL;
	}


	public static void main(String[] args) throws Exception
	{
		ConsoleUtil.disableFancy = true;
		SplMojo mojo = new SplMojo();
		//new File("../splData/data/splDraftFacts.txt.zip")
		mojo.facts = new File[] {new File("../splData/data/bwDraftFacts-export-20110627-2-fixed.txt.zip")};
		mojo.rxNormMapFile = new File("../splData/data/splRxNormMapData");
		mojo.splZipFilesFolder = new File("/media/truecrypt2/Source Data/SPL from BW/temp/");
		mojo.outputFileName = "splData.jbin";
		mojo.filterNda = false;
		mojo.loadFactsWithMissingSPL = true;
		mojo.outputDirectory = new File("../splData/target");
		
		mojo.execute();
	}
	
	
}
