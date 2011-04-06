package gov.va.akcds.wbSplFileExtractor;

import gov.va.akcds.util.time.Time;

import java.io.File;

public class SplFileExtractor {

	public static File extractSplFiles(File dir, String dataFileName)
			throws Exception {
		System.out.println("Extracting spl files.");
		System.out.println(Time.now());
		System.out.println(dir.getCanonicalPath());
		// data file
		File dataFile = new File(dir, dataFileName);
		File dstDir = new File(dir, "splZipFiles");
		if (dstDir.exists() == false) {
			dstDir.mkdirs();
		}
		// extract original zip file
		System.out.println("Extracting zip of zips.");
		System.out.println(Time.now());
		ZipUtil.unzip(dataFile, dstDir, true);
		// get extracted files
		File[] files = dstDir.listFiles();
		System.out.println("\nExtracting zips.");
		System.out.println("\nTotal files: " + files.length);
		System.out.println(Time.now());
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String srcFileName = file.getName().toLowerCase();
			if (srcFileName.endsWith(".zip") == false) {
				// skip anything that is not a zip file (e.g. .svn files)
				continue;
			}
			String dstFileName = srcFileName.substring(0, srcFileName
					.lastIndexOf(".zip"));
			File finalDstDir = new File(dir, "splXml/" + dstFileName);
			if (finalDstDir.exists() == false) {
				finalDstDir.mkdirs();
			}
			if (i % 10 == 0) {
				System.out.print(".");
			}
			if (i % 500 == 0) {
				System.out.println(" " + i + " zip files extracted.");
			}
			ZipUtil.unzip(file, finalDstDir);
		}
		return new File(dir, "splXml");
	}

}
