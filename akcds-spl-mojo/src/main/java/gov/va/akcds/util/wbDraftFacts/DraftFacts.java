package gov.va.akcds.util.wbDraftFacts;

import gov.va.akcds.util.ConsoleUtil;
import gov.va.akcds.util.snomedMap.SnomedCustomNameCodeMap;
import gov.va.akcds.util.wbDraftFacts.ManualTargetRemap.ReplacementTarget;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipInputStream;

/**
 * read in the draft facts, return groups of them with the same setId and version.  Prefers that the source data be sorted by setId and version, 
 * but it is ok if it isn't... just less efficient.
 * @author Daniel Armbrust
 */
public class DraftFacts implements Enumeration<ArrayList<DraftFact>> {

	private File[] sourceFiles_;
	private ArrayList<DraftFact> next_ = null;
	private int sourceFile_ = 0;
	private ZipInputStream zis_ = null;
	BufferedReader in_ = null;
	private DraftFact carryOver_ = null;
	
	private int draftFactCounter = 0;
	
	private SnomedCustomNameCodeMap scncm_ = null;
	private ManualTargetRemap mtr_ = null;
	private long snomedMapUse_ = 0;
	private long manualRemapUse_ = 0;
	
	/**
	 * Data file should be a zip file containing only the draft facts text file
	 */
	public DraftFacts(File[] dataFiles, SnomedCustomNameCodeMap scncm, ManualTargetRemap mtr) throws Exception
	{
		sourceFiles_ = dataFiles;
		scncm_ = scncm;
		mtr_ = mtr;
	}
	
	private void getNext() throws Exception
	{
		ArrayList<DraftFact> draftFacts = new ArrayList<DraftFact>();
		
		String currentSetIdVersion = null;
		
		if (carryOver_ != null)
		{
			currentSetIdVersion = carryOver_.getSplSetId() + ":" + carryOver_.getSplVersion();
			draftFacts.add(carryOver_);
			carryOver_ = null;
		}
		
		while (next_ == null)
		{
			if (sourceFiles_.length > sourceFile_)
			{
				if (zis_ == null)
				{
					ConsoleUtil.println("Reading " + sourceFiles_[sourceFile_]);
					zis_ = new ZipInputStream(new BufferedInputStream(new FileInputStream(sourceFiles_[sourceFile_])));
				}
			}
			else
			{
				if (draftFacts.size() > 0)
				{
					next_ = draftFacts;
				}
				return;
			}

			if (in_ == null)
			{
				if (zis_.getNextEntry() != null)
				{
					in_ = new BufferedReader(new InputStreamReader(zis_));
				}
				else
				{
					zis_.close();
					zis_ = null;
					sourceFile_++;
				}
			}
			
			while (in_ != null)
			{
				for (String str = in_.readLine(); str != null; str = in_.readLine())
				{
					if (str.trim().length() > 0)
					{
						DraftFact fact = new DraftFact(str, sourceFiles_[sourceFile_].getName());
						String setId = fact.getSplSetId();
						if (setId.equals("SPL_SET_ID"))
						{
							//Our header line from the file...
							continue;
						}
						
						fixFact(fact);
						
						draftFactCounter++;
						
						if (currentSetIdVersion == null)
						{
							currentSetIdVersion = setId + ":" + fact.getSplVersion();
						}
						if (!(setId + ":" + fact.getSplVersion()).equals(currentSetIdVersion))
						{
							carryOver_ = fact;
							next_ = draftFacts;
							return;
						}
						draftFacts.add(fact);
					}
				}
				in_ = null;
			}
		}
	}

	@Override
	public boolean hasMoreElements()
	{
		if (next_ == null)
		{
			try
			{
				getNext();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		if (next_ == null)
		{
			//cleanup
			carryOver_ = null;
			scncm_ = null;
			mtr_ = null;
			sourceFiles_ = null;
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<DraftFact> nextElement()
	{
		if (next_ == null)
		{
			try
			{
				getNext();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		if (next_ == null)
		{
			throw new NoSuchElementException();
		}
		ArrayList<DraftFact> temp = next_;
		next_ = null;
		return temp;
	}
	
	public int getTotalDraftFactCount()
	{
		return draftFactCounter;
	}
	
	private void fixFact(DraftFact fact)
	{
		if (mtr_ != null)
		{
			ReplacementTarget rt = mtr_.getRemap(fact.getRowId());
			if (rt != null)
			{
				fact.setDrugCode(rt.targetDrugCode);
				fact.setDrugName(rt.targetDrugName);
				manualRemapUse_++;
			}
		}
		if (scncm_ != null && fact.getConceptCode().equals("-"))
		{
			//See if we can map it using our extra map data.
			Integer code = scncm_.getCode(fact.getConceptName());
			if (code != null)
			{
				fact.setConceptCode(code.toString());
				snomedMapUse_++;
			}
		}
	}
	
	public long getSnomedUseCount()
	{
		return snomedMapUse_;
	}
	
	public long getManualRemapUseCount()
	{
		return manualRemapUse_;
	}
}
