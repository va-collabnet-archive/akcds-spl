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
import gov.va.akcds.util.fileUtil.StatsFilePrinter;
import gov.va.akcds.util.snomedMap.SnomedCustomNameCodeMap;
import gov.va.akcds.util.snomedMap.SnomedFullNameCodeMap;
import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;
import gov.va.akcds.util.wbDraftFacts.ManualTargetRemap;
import gov.va.akcds.util.zipUtil.ZipFileContent;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
	
	/**
	 * Location of the custom snomed mapping data.
	 * 
	 * @parameter
	 * @required
	 */
	private File snomedCustomMapFile;
	
	/**
	 * Location of the custom draft fact remap data.  Optional
	 * 
	 * @parameter
	 * @optional
	 */
	private File draftFactRemapFile;
	
	/**
	 * Location of the full snomed mapping data.  Optional
	 * 
	 * @parameter
	 * @optional
	 */
	private File snomedFullMapFile;

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
	private long droppedFactsForNot_ = 0;
	private int flagCurationDataForConflict_ = 0;
	private HashSet<String> uniqueTargetConcepts_ = new HashSet<String>();
	private ArrayList<String[]> mismatchedCurationStateErrors_ = new ArrayList<String[]>();
	
	//An uber mapping of unique SCT codes (real codes only) to (a set of) unique draft facts to the set of unique SPL Set IDs
	private Hashtable<String, Hashtable<String, HashSet<String>>> sctFactLabelCounts = new Hashtable<String, Hashtable<String, HashSet<String>>>();
	
	//And the same uber mapping for non-sct codes.
	private Hashtable<String, Hashtable<String, HashSet<String>>> nonSctFactLabelCounts = new Hashtable<String, Hashtable<String, HashSet<String>>>();
	
	//A mapping of splSetIds -> drug names
	Hashtable <String, String> reverseDrugMap = new Hashtable<String, String>();
	
	private boolean createLetterRoots_ = true;  //switch this to false to generate a flat structure under the spl root concept
	
	private Hashtable<String, Drug> splDrugConcepts_ = new Hashtable<String, Drug>();
	private Hashtable<UUID, EConcept> splSetIdConcepts_ = new Hashtable<UUID, EConcept>();
	
	private Set<UUID> nonSnomedTerms_ = new HashSet<UUID>();
	private UUID rootConceptUUID_, nonSnomedRootConceptUUID_;
	
//	private StatsFilePrinter cdifData_ = new StatsFilePrinter(new String[] {"Row ID", "Drug Name", "Role", "Target Code", "Sentence"}, "\t", "\r\n", 
//			new File(getOutputDirectory(), "cdifData.tsv"), "Cdif Data");

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
			//Load the snomed map data - used to fill in missing codes in the BW data.
			draftFacts = new DraftFacts(getFacts(), new SnomedCustomNameCodeMap(snomedCustomMapFile), new ManualTargetRemap(draftFactRemapFile));

		
			ConsoleUtil.println(new Date().toString());
			// process the zip of zips
			

			while (draftFacts.hasMoreElements())
			{
				ArrayList<DraftFact> currentDraftFacts = draftFacts.nextElement();
				//Each draftFact returned in a batch will have the same set id.
				//Each draftFact returned will have the same setId version.
				
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
			
//			//Done with this.
//			cdifData_.close();
			
			ConsoleUtil.println("");
			ConsoleUtil.println("Data loaded, filtered and normalized.  Found " + splDrugConcepts_.size() + " unique drugs.  Searching for RxNorm VUIDs");
			
			StatsFilePrinter distributionStats = new StatsFilePrinter(new String[] {"Drug Name", "SPL Count"}, "\t", "\r\n", 
					new File(getOutputDirectory(), "spl-distribution-stats.tsv"), "SPL Distribution Stats");
			
			addVUIDMappings();
			
			ConsoleUtil.println("Converting results to workbench format");
			
			//Reduce memory footprint
			ArrayList<Drug> drugs = new ArrayList<Drug>(splDrugConcepts_.size());
			
			Iterator<String> keys = splDrugConcepts_.keySet().iterator();
			while (keys.hasNext())
			{
				String key = keys.next();
				drugs.add(splDrugConcepts_.get(key));
				keys.remove();
			}
			splDrugConcepts_ = null;
			
			
			for (int j = 0; j < drugs.size(); j++)
			{
				Drug d = drugs.get(j);
				drugs.set(j, null);  //clear memory as we go
				distributionStats.addLine(new String[] {d.drugName, d.setIds.size() + ""});
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
					
					if (sdf.curationState != null)
					{
						if (sdf.curationState.equalsIgnoreCase("NEW"))
						{
							//If it still said NEW after all duplicates were added in, change it to reject.
							reject_++;
							sdf.curationState = "Reject";
						}
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
				for (String vuid : d.rxNormTradeNameVuids)
				{
					conceptUtility_.addAnnotation(concept, vuid, StaticDataType.RXNorm_VUID_TRADENAME.getUuid());
				}
				
				storeConcept(concept);
			}
			
			distributionStats.close();
			
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
			ConsoleUtil.println("SPL Drug Concepts created: " + drugs.size());
			ConsoleUtil.println("Unique set IDs processed / SPL Set ID concepts created: " + splSetIdConcepts_.size());
			ConsoleUtil.println("Processed " + draftFacts.getTotalDraftFactCount() + " draft facts");
			ConsoleUtil.println("Merged " + duplicateDraftFactMerged_ + " duplicate draft facts onto an existing draft fact.");
			ConsoleUtil.println("Loaded " + uniqueDraftFactCount_ + " draft facts");
			ConsoleUtil.println("Ignored " + droppedFactsForNot_ + " facts because the relationship started with 'not.'");
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
			ConsoleUtil.println("Facts marked as NEW which were remapped into reject: " + new_);
			ConsoleUtil.println("Facts flagged for review: " + flagCurationDataForConflict_);		
			ConsoleUtil.println("Manual draft fact remap used to correct " + draftFacts.getManualRemapUseCount() + " draft facts.");
			ConsoleUtil.println("Snomed map data used to correct " + draftFacts.getSnomedUseCount() + " draft fact instances");
			ConsoleUtil.println("Unique target concepts: " + uniqueTargetConcepts_.size());	
			ConsoleUtil.println("Unique real SCT concepts " + sctFactLabelCounts.size());
			ConsoleUtil.println("Created " + nonSnomedTerms_.size() + " non-snomed concepts");
			
			
			//Sanity Checks (detect stupid programmer errors...)
			{
				if ((conceptCounter_ - metadataConceptCounter_ - nonSnomedTerms_.size() - drugs.size()) != splSetIdConcepts_.size())
				{
					ConsoleUtil.printErrorln("Math error in counters!");
				}
				if (duplicateDraftFactMerged_ 
						+ uniqueDraftFactCount_ 
						+ skipDraftFactForWrongVersion_  
						+ (isFilterNda() ? droppedFactsForNoNDAs_ : 0)
						+ droppedFactsForNot_
						!= draftFacts.getTotalDraftFactCount())
				{
					ConsoleUtil.printErrorln("Programmer Error - draft fact numbers do not add up!");
				}
				
				if (accept_ + reject_ + flagCurationDataForConflict_ !=  uniqueDraftFactCount_)
				{
					ConsoleUtil.printErrorln("Programmer Error - draft fact state numbers do not add up!");
				}
				
				if (sctFactLabelCounts.size() + nonSnomedTerms_.size() != uniqueTargetConcepts_.size())
				{
					ConsoleUtil.printErrorln("Programmer Error - target counts do not add up!");
				}
				
				if (nonSnomedTerms_.size() != nonSctFactLabelCounts.size())
				{
					ConsoleUtil.printErrorln("Programmer Error - non-snomed concept counts do not add up.!");
				}
			
			}
			
			
			//Clear some memory.
			splSetIdConcepts_ = null;
			SnomedFullNameCodeMap sfncm = new SnomedFullNameCodeMap(snomedFullMapFile);
			
			{
				StatsFilePrinter sfp = new StatsFilePrinter(new String[] {"code", "concept name", "unique draft facts", "unique label count", "unique drug count"},
						"\t", "\r\n", new File(getOutputDirectory(), "sct-stats.tsv"), "Load stats");
				
				for (Map.Entry<String, Hashtable<String, HashSet<String>>> x : sctFactLabelCounts.entrySet())
				{
					//Find all of the unique drugs that any involved setId points to.
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
					
					String name = sfncm.getName(x.getKey());
					sfp.addLine(new String[] {x.getKey(), (name == null ? "" : name), factAndLabel.size() + "", labelTotal + "", uniqueDrugs.size() + ""});
				}
				sfp.close();
			}
			
			//Print the same stats for the non-snomed concepts.
			{
				StatsFilePrinter sfp = new StatsFilePrinter(new String[] {"concept name", "unique draft facts", "unique label count", "unique drug count"},
						"\t", "\r\n", new File(getOutputDirectory(), "nonsct-stats.tsv"), "Load stats");
				
				for (Map.Entry<String, Hashtable<String, HashSet<String>>> x : nonSctFactLabelCounts.entrySet())
				{
					//Find all of the unique drugs that any involved setId points to.
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
					
					sfp.addLine(new String[] {x.getKey(), factAndLabel.size() + "", labelTotal + "", uniqueDrugs.size() + ""});
				}
				sfp.close();
			}
			
			
			if (mismatchedCurationStateErrors_.size() > 0)
			{
				StatsFilePrinter sfp = new StatsFilePrinter(new String[] {"unique key", "previously found state", "newly found state"},
						"\t", "\r\n", new File(getOutputDirectory(), "curationErrors.tsv"), "curation state mismatches");
				
				for (String[] s : mismatchedCurationStateErrors_)
				{
					sfp.addLine(s);
				}
				sfp.close();
			}
			sdh.dumpIndex(getOutputDirectory());
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

		
		UUID setIdUUID = getUUIDForSpl(spl);
		String version = null;
		
		//Lots of version checking validation...
		if (splDraftFacts.size() > 0)
		{
			//All draft facts in a set will have the same version number.
			DraftFact fact = splDraftFacts.get(0);

			// default version to that of current doc, for npl data as we don't know what it is
			if (fact.getSplVersion().equals("-"))
			{
				version = spl.getVersion();
			}
			else
			{
				version = fact.getSplVersion();
			}
			
			//Check and see if we have already loaded a different version of this SPL
			//We have some bad data coming in which causes this to happen.  We only want to keep
			//the newest version.  This actually adds quite a bit of complexity, to handle removing
			//ones that we already added (but shouldn't have)
			
			EConcept concept = splSetIdConcepts_.get(setIdUUID);
			if (concept != null)
			{
				//We already created this concept.  Version check...
				TkConceptAttributes attribs = concept.getConceptAttributes();
				String loadedVersion = "";
				for (TkRefsetAbstractMember<?> x : attribs.getAnnotations())
				{
					if (x.getRefsetUuid().equals(StaticDataType.SPL_VERSION.getUuid()))
					{
						loadedVersion = ((TkRefsetStrMember)x).getStrValue();
						break;
					}
				}
				
				int newVersion = Integer.parseInt(version);
				int oldVersion = Integer.parseInt(loadedVersion);
				
				if (newVersion > oldVersion)
				{
					int clearedCount = 0;
					//We need to clear the old one (and all of the corresponding draft fact) and store this new one.
					concept = null;
					
					splSetIdConcepts_.remove(setIdUUID);
					
					//Need to get all of the "drugs" that this splSet Id was involved with, and go through each of them, 
					//removing bad facts.
					String drugName = reverseDrugMap.get(spl.getSetId());
					Drug drug = splDrugConcepts_.get(drugName);
					
					Iterator<SimpleDraftFact> sdfi = drug.draftFacts.values().iterator();
					
					while (sdfi.hasNext())
					{
						SimpleDraftFact sdf = sdfi.next();
						Iterator<SimpleDraftFactSource> sdfsi = sdf.sources.iterator();
						while (sdfsi.hasNext())
						{
							SimpleDraftFactSource sdfs = sdfsi.next();
							if (sdfs.setId.equals(spl.getSetId()) && !sdfs.version.equals(version))
							{
								sdfsi.remove();
								clearedCount++;
								duplicateDraftFactMerged_--;
							}
						}
						//If there are no sources left... then this entire draft fact shouldn't have been created.  Need to un-create it.
						if (sdf.sources.size() == 0)
						{
							uniqueDraftFactCount_--;
							duplicateDraftFactMerged_++;  //it wasn't a duplicate.  Increment this back, decrement unique count instead.
							if (sdf.curationState.equalsIgnoreCase("New"))
							{
								new_--;
							}
							else if (sdf.curationState.equalsIgnoreCase("Accept"))
							{
								accept_--;
							}
							else if (sdf.curationState.equalsIgnoreCase("Reject"))
							{
								reject_--;
							}
							else if (sdf.curationState.equalsIgnoreCase("flag"))
							{
								flagCurationDataForConflict_--;
								int remove = -1;
								for (int i = 0; i < mismatchedCurationStateErrors_.size(); i++)
								{
									if (mismatchedCurationStateErrors_.get(i)[0].equals(sdf.getUniqueKey()))
									{
										remove = i;
										break;
									}
								}
								if (remove >= 0)
								{
									mismatchedCurationStateErrors_.remove(remove);
								}
							}
							sdfi.remove();
						}
					}
					
					//Seems highly unlikely that this drug would end up with no draft facts at all, 
					//so I'm not implementing that cleanup logic.
					ConsoleUtil.printErrorln("Removing " + clearedCount + " facts for " + spl.getSetId() 
							+ " from version " + oldVersion + " because we found " + newVersion);
						skipDraftFactForWrongVersion_ += clearedCount;
				}
				else if (newVersion < oldVersion)
				{
					ConsoleUtil.printErrorln("Ignoring " + splDraftFacts.size() + " facts for " + spl.getSetId() 
							+ " from version " + newVersion + " because we already have " + oldVersion);
					skipDraftFactForWrongVersion_ += splDraftFacts.size();
					return;  //dont add this batch
				}
			}
			
			//Create the SPL concept in workbench format
			if (concept == null)
			{
				concept = createSetId(spl);
			}
		}
		
		for (int i = 0; i < splDraftFacts.size(); i++)
		{
			DraftFact fact = splDraftFacts.get(i);
			
			//This will ensure that all setIds in this batch have the same version
			if (!fact.getSplVersion().equals("-") && !fact.getSplVersion().equals(version))
			{
				//This should be impossible the way that they are batched coming into this method.
				throw new Exception("Programming error - different version in one batch of draft facts");
			}
						
			//Ok, we want to load this one.  See if we have already started it.
			String drugName = fact.getDrugName().toUpperCase();
			
			if (skipBadDataFromBatchOne(fact, drugName, splDraftFacts))
			{
				break;
			}
			
			Drug drug = splDrugConcepts_.get(drugName);
			
			if (drug == null)
			{
				drug = new Drug(drugName);
				splDrugConcepts_.put(drugName, drug);
			}
			
			//Also load this mapping hashtable (used for stats, also used for "undos")
			reverseDrugMap.put(fact.getSplSetId(), drugName);
			
			drug.setIdUUIDs.add(setIdUUID);
			drug.setIds.add(fact.getSplSetId());
			
			//Now, set up this draft fact
			SimpleDraftFact newSdf = new SimpleDraftFact(drugName, fact.getRoleName(), (fact.getConceptCode().equals("-") ? fact.getConceptName() : fact.getConceptCode()));
			
			if (newSdf.relName.toLowerCase().startsWith("not."))
			{
				droppedFactsForNot_++;
				continue;
			}
			
//			//Cdif check
//			if (newSdf.targetCode.equals("62315008"))
//			{
//				if (fact.getSentence().toLowerCase().indexOf("clostridium difficile") >= 0 ||
//						fact.getSentence().toLowerCase().indexOf("c. difficile") >= 0 ||
//						fact.getSentence().toLowerCase().indexOf("cdad") >= 0)
//				{
//					cdifData_.addLine(new String[] {fact.getRowId(), drugName, newSdf.relName, newSdf.targetCode, fact.getSentence()});
//				}
//			}
			
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
			{
				Hashtable<String, HashSet<String>> stats = null;
				if (fact.getConceptCode().length() > 1)
				{
					
					//A real SCT code.
					stats = sctFactLabelCounts.get(fact.getConceptCode());
					if (stats == null)
					{
						stats = new Hashtable<String, HashSet<String>>();
						sctFactLabelCounts.put(fact.getConceptCode(), stats);
					}
				}
				else
				{
					stats = nonSctFactLabelCounts.get(fact.getConceptName());
					if (stats == null)
					{
						stats = new Hashtable<String, HashSet<String>>();
						nonSctFactLabelCounts.put(fact.getConceptName(), stats);
					}
				}
					
				HashSet<String> labels = stats.get(existingSdf.getUniqueKey());
				if (labels == null)
				{
					labels = new HashSet<String>();
					stats.put(existingSdf.getUniqueKey(), labels);
				}
				labels.add(fact.getSplSetId());
			}
				
			//end of other stats...
			
			//Add on the unique draft fact data for this instance of the draft fact
			SimpleDraftFactSource sdfs = new SimpleDraftFactSource(fact.getRowId(), fact.getSplSetId(), version, fact.getSecName(), fact.getSentence(), fact.getDrugCode());
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
							/*
							 * The data now includes non-curated assertions.  
							 * If a label has the same triple and it has been accepted or rejected, 
							 * do not count any "New" as state mis-matches. In other words, you only count 
							 * a state-mismatch if there is an Accept and Reject. For triples that are uniquely 
							 * “NEW” (i.e., the same triple for that compound has not been accepted or rejected 
							 * elsewhere) load it into the WB with a state = Reject 
							 */
						
							if (existingSdf.curationState.equalsIgnoreCase("New"))
							{
								//If it is currently new - and we now have something more specific, switch it to the new value.
								existingSdf.curationState = fact.getCurationState();
								new_--;
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
								else
								{
									ConsoleUtil.printErrorln("Unexpected curation state during switch from new: '" + fact.getCurationState() + "'");
								}
							}
							else if (fact.getCurationState().equalsIgnoreCase("NEW"))
							{
								//If the new one is set to "new", but the old one is set to something else, just ignore the 
								//this 'new' value.  The existing value is better.
							}
							else
							{
								mismatchedCurationStateErrors_.add(new String[] {existingSdf.getUniqueKey(), existingSdf.curationState, fact.getCurationState()});
								if (existingSdf.curationState.equalsIgnoreCase("Accept"))
								{
									accept_--;
								}
								else if (existingSdf.curationState.equalsIgnoreCase("Reject"))
								{
									reject_--;
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
	}
	
	private boolean skipBadDataFromBatchOne(DraftFact fact, String drugName, ArrayList<DraftFact> splDraftFacts)
	{
		if (fact.getFromFileName().indexOf("bwDraftFacts-B1-export-20110629-5") < 0)
		{
			//only check if it is from batch 1.
			return false;
		}
		if (drugName.equals("HYOSCYAMINE & SCOPOLAMINE & PHENOBARBITAL & ATROPINE"))
		{
			ConsoleUtil.println("Skipping " + drugName + " - " + splDraftFacts.size());
			for (DraftFact d : splDraftFacts)
			{
				if (!d.getCurationState().equalsIgnoreCase("NEW"))
				{
					System.out.println(d.getCurationState());
				}
			}
			skipDraftFactForWrongVersion_ += splDraftFacts.size();
			return true;
		}
		if (drugName.equals("INTERFERON ALFA-2A"))
		{
			ConsoleUtil.println("Skipping " + drugName + " - " + splDraftFacts.size());
			for (DraftFact d : splDraftFacts)
			{
				if (!d.getCurationState().equalsIgnoreCase("NEW"))
				{
					System.out.println(d.getCurationState());
				}
			}
			skipDraftFactForWrongVersion_ += splDraftFacts.size();
			return true;
		}
		if (drugName.equals("MECHLORETHAMINE"))
		{
			ConsoleUtil.println("Skipping " + drugName + " - " + splDraftFacts.size());
			for (DraftFact d : splDraftFacts)
			{
				if (!d.getCurationState().equalsIgnoreCase("NEW"))
				{
					System.out.println(d.getCurationState());
				}
			}
			skipDraftFactForWrongVersion_ += splDraftFacts.size();
			return true;
		}
		if (drugName.equals("OXAPROZIN"))
		{
			ConsoleUtil.println("Skipping " + drugName + " - " + splDraftFacts.size());
			for (DraftFact d : splDraftFacts)
			{
				if (!d.getCurationState().equalsIgnoreCase("NEW"))
				{
					System.out.println(d.getCurationState());
				}
			}
			skipDraftFactForWrongVersion_ += splDraftFacts.size();
			return true;
		}
		if (drugName.equals("TILMICOSIN"))
		{
			ConsoleUtil.println("Skipping " + drugName + " - " + splDraftFacts.size());
			for (DraftFact d : splDraftFacts)
			{
				if (!d.getCurationState().equalsIgnoreCase("NEW"))
				{
					System.out.println(d.getCurationState());
				}
			}
			skipDraftFactForWrongVersion_ += splDraftFacts.size();
			return true;
		}
		
		return false;
	}
	
	private UUID getUUIDForSpl(Spl spl)
	{
		return UUID.nameUUIDFromBytes((uuidRoot_ + ":root:setIds:" + spl.getSetId()).getBytes());
	}
	
	/**
	 * This should only be called when the spl concept for this doc has not yet been created.
	 */
	private EConcept createSetId(Spl spl) throws Exception
	{
		UUID setIdUUID = getUUIDForSpl(spl);
		
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
		EConcept c = splSetIdConcepts_.put(setIdUUID, concept);
		if (c != null)
		{
			throw new Exception("Created a new spl concept one an old one already existed.  Programming error");
		}
		return concept;
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
		
		StatsFilePrinter sfp = new StatsFilePrinter(new String[] {"Drug Name", "SPL -> VUID", "SPL -> Tradename VUID", "NDC -> VUID", "Unique VUIDs", "Unique Tradename VUIDS"},
				"\t", "\r\n", new File(getOutputDirectory(), "mappingStats.tsv"), "VUID mapping statistics");
		
		StatsFilePrinter splNotInRXNorm_ = new StatsFilePrinter(new String[] { "SPL Concept Name", "Set ID" }, "\t", "\r\n", 
				new File(getOutputDirectory(), "SPL_Not_in_RXNorm.tsv"), "SPL Not in RXNorm");
			
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
			int foundBySplTradename = 0;
			int foundByNDC = 0;
			for (String setId : d.setIds)
			{
				SplAsKey sak = rxNormMaps.getSplAsKey().get(setId);
				if (sak != null)
				{
					for (SplAsKeyData sakd : sak.getCodes())
					{
						d.rxNormVuids.addAll(sakd.getVuid());
						d.rxNormTradeNameVuids.addAll(sakd.getTradenameOfVuids());
						foundBySpl += sakd.getVuid().size();
						foundBySplTradename += sakd.getTradenameOfVuids().size();
					}
				}
				else
				{
					splNotInRXNorm_.addLine(new String[] {d.drugName, setId});
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
			sfp.addLine(new String[] {d.drugName, foundBySpl + "", foundBySplTradename + "", foundByNDC + "",
					d.rxNormVuids.size() + "", d.rxNormTradeNameVuids.size() + ""});
		}
		sfp.close();
		splNotInRXNorm_.close();
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
		mojo.facts = new File[] {new File("/media/truecrypt2/Source Data/Draft Facts/bwDraftFacts-B1-export-20110629-5.txt.zip"), 
				new File("/media/truecrypt2/Source Data/Draft Facts/bwDraftFacts-B2-export-20110707-1.txt.zip")};
		mojo.rxNormMapFile = new File("../splData/data/splRxNormMapData");
		mojo.snomedCustomMapFile = new File("../splData/data/snomedCustomNameCodeMap.txt");
		mojo.snomedFullMapFile = new File("../splData/data/snomedCodeNameMap.txt");
		mojo.splZipFilesFolder = new File("/media/truecrypt2/Source Data/SPL from BW/");
		mojo.outputFileName = "splData.jbin";
		mojo.filterNda = false;
		mojo.loadFactsWithMissingSPL = true;
		mojo.outputDirectory = new File("../splData/target");
		
		mojo.execute();
	}
	
	
}
