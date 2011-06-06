package gov.va.akcds.util.wbDraftFacts;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

public class DraftFacts {

	//
	// N.B.
	// Draft facts are written out to files because pulling all 1.5 million into
	// memory caused heap overflow errors.
	//

	//
	// instance variables
	//

	private File draftFactsRoot_;

	/**
	 * Data file should be a zip file containing only the draft facts text file
	 */
	public DraftFacts(File dataFile, File expansionFolder) throws Exception
	{
		draftFactsRoot_ = expansionFolder;
		draftFactsRoot_.mkdirs();
		
		for (File f : draftFactsRoot_.listFiles())
		{
			f.delete();
		}
		
		init(dataFile);
	}

	public void init(File dataFile) throws Exception {
		System.out.println("Reorganizing draft facts by set id");

		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
		//Point to the first (and only) expected file in this zip file
		zis.getNextEntry();
		
		BufferedReader in =  new BufferedReader(new InputStreamReader(zis));
		String prevSplSetId = "";
		File outFile = null;
		BufferedWriter out = null;
		int cnt = 0;
		for (String str = in.readLine(); str != null; str = in.readLine()) {
			cnt++;
			if (str.trim().length() > 0) {
				DraftFact fact = new DraftFact(str);
				String splSetId = fact.getSplSetId();
				if (splSetId != null && splSetId.equals(prevSplSetId) == false) {
					if (out != null) {
						out.close();
					}
					outFile = new File(draftFactsRoot_, splSetId + ".txt");
					out = new BufferedWriter(new FileWriter(outFile));
					prevSplSetId = splSetId;
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
		System.out.println("\nDone organizing draft facts.");
	}

	/**
	 * 
	 * Method to get the facts for a given splSetId.
	 * 
	 * @param splSetId
	 * @return
	 * 
	 */

	public ArrayList<DraftFact> getFacts(String splSetId) throws Exception {
		ArrayList<DraftFact> rtn = new ArrayList<DraftFact>();
		File file = new File(draftFactsRoot_, splSetId + ".txt");
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
