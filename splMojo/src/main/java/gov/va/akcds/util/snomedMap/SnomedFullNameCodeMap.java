package gov.va.akcds.util.snomedMap;

import gov.va.akcds.util.ConsoleUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This class reads a tab-delimited file that looks like this:
 * 
 * Snomed Code		Legacy Code		PreferredTerm		Fully Specified Name
 * 
 * The class GenerateSnomedMapFile will build this file from DTS.
 * 
 * @author Daniel Armbrust
 */
public class SnomedFullNameCodeMap
{
	private Hashtable<String, String> codeToName_ = new Hashtable<String, String>();
	
	public SnomedFullNameCodeMap(File mapData) throws IOException
	{
		if (mapData != null && mapData.exists())
		{
			ConsoleUtil.println("Reading full snomed map file: " + mapData + " to get concept names to add to the stats output.");
		}
		else
		{
			ConsoleUtil.println("File " + mapData + " does not exist.  Will not write snomed concept names in stats file");
			return;
		}
		BufferedReader in = new BufferedReader(new FileReader(mapData));

		for (String str = in.readLine(); str != null; str = in.readLine())
		{
			String temp = str.trim();
			if (temp.startsWith("#"))
			{
				continue;
			}
			String[] t1 = temp.split("\t");
			codeToName_.put(t1[0], t1[3]);
		}
		
	
		ConsoleUtil.println("Snomed name-code map built - contains " + codeToName_.size() + " codes");
	}
	
	public String getName(String code)
	{
		return codeToName_.get(code);
	}
	
	public static void main(String[] args) throws IOException
	{
		SnomedFullNameCodeMap sm = new SnomedFullNameCodeMap(new File("../splData/data/snomedCodeNameMap.txt"));
		System.out.println(sm);
	}
}
