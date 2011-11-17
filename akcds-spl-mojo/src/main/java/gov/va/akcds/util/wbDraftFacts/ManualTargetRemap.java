package gov.va.akcds.util.wbDraftFacts;

import gov.va.akcds.util.ConsoleUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

/**
 * The file this class reads simply looks for a csv file with 
 * 
 * rowId, replacementTargetCode, replacementTargetCodeText
 * 
 * @author Daniel Armbrust
 */
public class ManualTargetRemap
{
	private Hashtable<String, ReplacementTarget> rowIDRemaps = new Hashtable<String, ReplacementTarget>();
	
	public ManualTargetRemap(File mapData) throws IOException
	{
		if (mapData == null || !mapData.exists())
		{
			ConsoleUtil.print("No draft fact remap data supplied");
			return;
		}
		
		ConsoleUtil.println("Reading draft fact remap data from " + mapData.getAbsolutePath());
		
		BufferedReader in = new BufferedReader(new FileReader(mapData));
		
		for (String str = in.readLine(); str != null; str = in.readLine())
		{
			String line = str.trim();
			if (line.startsWith("#"))
			{
				continue;
			}
			if (line.length() > 0)
			{
				String[] parts = line.split(","); 
				if (parts.length == 3)
				{
					rowIDRemaps.put(parts[0], new ReplacementTarget(parts[1], parts[2]));
				}
				else
				{
					System.err.println("Bad data in rowIDRemaps file");
				}
			}
		}
		
		ConsoleUtil.println("Draft fact remap built - contains " + rowIDRemaps.size() + " entries");
	}
	
	public ReplacementTarget getRemap(String rowId)
	{
		return rowIDRemaps.get(rowId);
	}
	
	public class ReplacementTarget
	{
		public String targetDrugCode;
		public String targetDrugName;
		
		public ReplacementTarget(String targetDrugCode, String targetDrugName)
		{
			this.targetDrugCode = targetDrugCode;
			this.targetDrugName = targetDrugName;
		}
	}
}
