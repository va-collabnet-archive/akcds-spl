package gov.va.akcds.util.wbDraftFacts;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import org.apache.maven.plugin.MojoExecutionException;

public class DraftFacts {

	private File draftFactsRoot_;
	
	private Connection con;

	/**
	 * Data file should be a zip file containing only the draft facts text file
	 */
	public DraftFacts(File[] dataFiles, File expansionFolder) throws Exception
	{
		draftFactsRoot_ = expansionFolder;
		draftFactsRoot_.mkdirs();
		
		for (File f : draftFactsRoot_.listFiles())
		{
			f.delete();
		}
		
		for (File dataFile : dataFiles)
		{
			init(dataFile);
		}
	}
	
	public void init(File dataFile) throws Exception {
		System.out.println("Reorganizing draft facts by set id : "+dataFile);

		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
		//Point to the first (and only) expected file in this zip file
		zis.getNextEntry();
						
		BufferedReader in =  new BufferedReader(new InputStreamReader(zis));
		String prevSetId = "";
		File outFile = null;
		BufferedWriter out = null;
		int cnt = 0;
		
		for (String str = in.readLine(); str != null; str = in.readLine()) {
			cnt++;
			if (str.trim().length() > 0) {
				DraftFact fact = new DraftFact(str);				
				String setId = fact.getSplSetId();
				
				if (setId != null && !setId.equals(prevSetId)) {
					if (out != null) {
						out.close();
					}
					outFile = new File(draftFactsRoot_, setId + ".txt");
					out = new BufferedWriter(new FileWriter(outFile, true));
					prevSetId = setId;
				}
				out.write(str + "\n");
			}
			if (cnt % 1000 == 0) {
				System.out.print(".");
			}
			if (cnt % 50000 == 0) {
				System.out.println("");
			}
		}
		System.out.println("\nDone organizing draft facts:"+cnt);
	}

	/**
	 * 
	 * Method to get the facts for a given spl dir name.
	 * 
	 * @param splSetId
	 * @return
	 * 
	 */

	public ArrayList<DraftFact> getFacts(String setId) throws Exception {
		ArrayList<DraftFact> rtn = new ArrayList<DraftFact>();
		File file = new File(draftFactsRoot_, setId + ".txt");
		if (file != null && file.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(file));
			for (String str = in.readLine(); str != null; str = in.readLine()) {
				if (str.trim().length() > 0) {
					DraftFact fact = new DraftFact(str);
					rtn.add(fact);
				}
			}
		}
		return rtn;
	}

}
