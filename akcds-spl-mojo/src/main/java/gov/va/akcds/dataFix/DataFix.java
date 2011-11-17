package gov.va.akcds.dataFix;

import gov.va.akcds.util.wbDraftFacts.DraftFact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DataFix
{

	/**
	 * A hack tool to reprocess flawed draft fact data.
	 * 
	 * Currently, removes reviews for old version, and strips out the "not." draft facts.
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		File factsIn = new File("../../../../Source Data/Draft Facts/bwDraftFacts-B1-export-20110627-2.txt.zip");
		File factsOut = new File("../../../../Source Data/Draft Facts/bwDraftFacts-B1-export-20110627-2-fixed2.txt.zip");

		long notDrop = 0;
		
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(factsOut)));

		
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(factsIn)));

		Hashtable<String, ArrayList<DraftFact>> facts = new Hashtable<String, ArrayList<DraftFact>>();
		HashSet<String> setIdsWithMultVer = new HashSet<String>();
		
		ZipEntry ze = zis.getNextEntry();
		if (ze != null)
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(zis));

			for (String str = in.readLine(); str != null; str = in.readLine())
			{
				if (str.trim().length() > 0)
				{
					DraftFact fact = new DraftFact(str, factsIn.getName());
					
					if (fact.getRoleName().startsWith("not."))
					{
						notDrop++;
						continue;
					}
					
					ArrayList<DraftFact> setIdFacts = facts.get(fact.getSplSetId().toUpperCase());
					if (setIdFacts == null)
					{
						setIdFacts = new ArrayList<DraftFact>();
						facts.put(fact.getSplSetId().toUpperCase(), setIdFacts);
					}
					
					if (setIdFacts.size() == 0)
					{
						setIdFacts.add(fact);
					}
					else if (setIdFacts.get(0).getSplVersion().equals(fact.getSplVersion()))
					{
						setIdFacts.add(fact);
					}
					else
					{
						//different versions... only keep newest fact(s).
						int existingVer = Integer.parseInt(setIdFacts.get(0).getSplVersion());
						int newVer = Integer.parseInt(fact.getSplVersion());
						
						if (newVer > existingVer)
						{
							setIdFacts.clear();
							setIdFacts.add(fact);
							setIdsWithMultVer.add(fact.getSplSetId().toUpperCase());
						}
						else
						{
							setIdsWithMultVer.add(fact.getSplSetId().toUpperCase());
						}
					}
				}
			}
			
			

			ZipEntry outputEntry = new ZipEntry(ze.getName());
			zos.putNextEntry(outputEntry);
			
			for (ArrayList<DraftFact> dfs : facts.values())
			{
				for (DraftFact df : dfs)
				{
					zos.write(new String(df.format() + "\r\n").getBytes());
				}
			}
			
			zos.closeEntry();
		}
		
		zis.close();
		zos.close();
		
		System.out.println("Draft Facts dropped for 'not': " + notDrop);
		System.out.println("Set Ids with mult versions: " + setIdsWithMultVer.size());
	}

}
	