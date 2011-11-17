package gov.va.akcds.util.snomedMap;

import gov.va.akcds.util.ConsoleUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * The file this class reads simply looks for this pattern:
 * 
 * text
 * 1234
 * another code text
 * 43578
 * 
 * Any data that doesn't fit this pattern, is deemed invalid, and ignored.
 * 
 * @author Daniel Armbrust
 */
public class SnomedCustomNameCodeMap
{
	private Hashtable<String, Integer> nameToCode_ = new Hashtable<String, Integer>();
	
	public SnomedCustomNameCodeMap(File mapData) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(mapData));
		
		ArrayList<String> data = new ArrayList<String>();
		for (String str = in.readLine(); str != null; str = in.readLine())
		{
			String temp = str.trim().toLowerCase();
			if (temp.length() > 0)
			{
				data.add(temp);
			}
		}
		
		while (data.size() > 1)
		{
			String name = data.remove(0);
			
			try 
			{
				Integer.parseInt(name);
				continue; 
			}
			catch (NumberFormatException e)
			{
				//noop
			}

			//Now get the number.
			String code = data.remove(0);
			Integer codeValue = null;
			
			try 
			{
				codeValue = Integer.parseInt(code);
			}
			catch (NumberFormatException e)
			{
				//should have been a code.  Wasn't... so toss.
				continue;
			}
			
			//Both the name and code are ok.  Now, need to peek ahead, and make sure the next line is a string (not a number)
			
			if (data.size() > 0)
			{
				try
				{
					Integer.parseInt(data.get(0));
					//Should have been a string.  This entry is invalid.  Skip.
					continue;
				}
				catch (NumberFormatException e) 
				{
					//noop
				}
			}

			nameToCode_.put(name, codeValue);			
		}
		
		ConsoleUtil.println("Snomed name-code map built - contains " + nameToCode_.size() + " codes");
	}
	
	public Integer getCode(String name)
	{
		return nameToCode_.get(name.trim().toLowerCase());
	}
	
	public static void main(String[] args) throws IOException
	{
		SnomedCustomNameCodeMap sm = new SnomedCustomNameCodeMap(new File("../splData/data/snomedCustomNameCodeMap.txt"));
		System.out.println(sm);
	}
}
