package gov.va.akcds.splMojo;

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
import java.util.Hashtable;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;

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
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;

	private EConceptUtility conceptUtility_ = new EConceptUtility(uuidRoot_);

	private DraftFacts draftFacts;
	
	private Hashtable<String, DynamicDataType> dynamicDataTypes_ = new Hashtable<String, DynamicDataType>();
	private UUID ndaTypeRoot_;

	private DataOutputStream dos_;
	private long conceptCounter_ = 0;
	private long xmlFileCnt_ = 0;

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
			if (outputDirectory.exists() == false)
			{
				outputDirectory.mkdirs();
			}

			// jbin (output) file
			File jbinFile = new File(outputDirectory, "splData.jbin");
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(jbinFile)));

			createMetaData();

			// Create the root concept (named SPL)
			EConcept rootConcept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":root").getBytes()), "SPL",
					System.currentTimeMillis());
			conceptUtility_.addDescription(rootConcept, "put version here",  StaticDataType.VERSION.getUuid()); // TODO add version

			storeConcept(rootConcept);

			// TODO break up by letter?

			System.out.println("Created " + conceptCounter_ + "metadata concepts");
			conceptCounter_ = 0;

			// get the data directory
			File dataDir = new File(outputDirectory.getParentFile(), "data");

			// load the draft facts
			System.out.println("Loading draft facts:");
			File draftFactsFile = new File(dataDir, "splDraftFacts.txt");
			draftFacts = new DraftFacts(draftFactsFile, new File(outputDirectory, "draftFactsByID"));

			// source file (splSrcData.zip is a zip of zips)
			String dataFileName = "splSrcData.zip";

			System.out.println(new Date().toString());
			System.out.println("Reading spl zip file");
			// process the zip of zips
			ZipContentsIterator outerZCI = new ZipContentsIterator(new File(dataDir, dataFileName));

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
						writeEConcept(spl, rootConcept.getPrimordialUuid());
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

			// write the meta data concepts
			System.out.println("TOTAL SPL FILES:   " + xmlFileCnt_);
			System.out.println("TOTAL concepts created: " + conceptCounter_);

			dos_.flush();
			dos_.close();

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
		writeAuxEConcept(metaDataRoot, "VA AKCDS Metadata", "", archRoot);

		UUID typesRoot = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types").getBytes());
		writeAuxEConcept(typesRoot, "Types", "", metaDataRoot);
		
		ndaTypeRoot_ = UUID.nameUUIDFromBytes((uuidRoot_ + ":metadata:types:ndaTypes").getBytes());
		writeAuxEConcept(ndaTypeRoot_, "NDA Types", "", typesRoot);

		for (StaticDataType dt : StaticDataType.values())
		{
			writeAuxEConcept(dt.getUuid(), dt.getNiceName(), dt.getDescription(), typesRoot);
		}
	}

	private void writeEConcept(Spl spl, UUID parentConceptUUID) throws Exception
	{
		// get the facts
		ArrayList<DraftFact> splDraftFacts = draftFacts.getFacts(spl.getSetId());

		// if there are no facts don't add the spl (it complicates things)
		if (splDraftFacts.size() > 0)
		{
			//For drug name and drug code, we just use the values from the first draft fact.  all draft facts should have the same value for these fields.
			EConcept concept = conceptUtility_.createConcept(UUID.nameUUIDFromBytes((uuidRoot_ + ":" + spl.getSetId()).getBytes()),
					splDraftFacts.get(0).getDrugName(), System.currentTimeMillis());

			conceptUtility_.addAdditionalId(concept, spl.getSetId(), StaticDataType.SET_ID.getUuid());
			conceptUtility_.addAdditionalId(concept, splDraftFacts.get(0).getDrugCode(), StaticDataType.DRUG_CODE.getUuid());
			
			conceptUtility_.addDescription(concept, splDraftFacts.get(0).getDrugName(), StaticDataType.DRUG_NAME.getUuid());

			// add an annotation to the conceptAttributes for each draft fact
			if (splDraftFacts != null)
			{
				for (int i = 0; i < splDraftFacts.size(); i++)
				{
					DraftFact fact = splDraftFacts.get(i);
					String annotationString = "";
					annotationString += fact.getRoleName() + "|";
					annotationString += fact.getRoleId() + "|";
					annotationString += fact.getConceptName() + "|";
					annotationString += fact.getConceptCode() + "|";
					conceptUtility_.addAnnotation(concept, annotationString, StaticDataType.DRAFT_FACT.getUuid());
				}
			}

			conceptUtility_.addAnnotation(concept, spl.getXMLFileAsString(), StaticDataType.SPL_XML_TEXT.getUuid());
//			if (spl.getXMLFileLength() > 64000)
//			{
//				System.out.println("long value: " + splDraftFacts.get(0).getDrugName());
//			}

			ArrayList<ZipFileContent> media = spl.getSupportingFiles();
			for (ZipFileContent zfc : media)
			{
				String fileName = zfc.getName();
				int splitPos = fileName.lastIndexOf('.');
				String extension = ((splitPos + 1 <= fileName.length() ? fileName.substring(splitPos + 1) : ""));
				conceptUtility_.addMedia(concept, StaticDataType.IMAGE.getUuid(), zfc.getFileBytes(), extension, fileName);
			}
			
			for (String nda : spl.getUniqueNDAs())
			{
				int split = 0;
				for (int i = 0; i < nda.length(); i++)
				{
					if (Character.isDigit(nda.charAt(i)))
					{
						split = i;
						break;
					}
				}
				String type = nda.substring(0, split);
				String value = nda.substring(split, nda.length());
				DynamicDataType ddt = getType(type);
				conceptUtility_.addAnnotation(concept, value, ddt.getIdentifier());
			}
			

			conceptUtility_.addRelationship(concept, parentConceptUUID, null);

			storeConcept(concept);
		}
	}
	
	private DynamicDataType getType(String type) throws Exception
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

	public static void main(String[] args) throws MojoExecutionException, Exception
	{
		SplMojo sm = new SplMojo();
		sm.outputDirectory = new File("../splData/target/");
		sm.execute();
	}
}
