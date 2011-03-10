package gov.va.akcds.wbSplFileExtractor;

import java.io.File;

public class SplFileExtractor {

	public static File extractSplFiles(File dir, String dataFileName) throws Exception {
		System.out.println("Extracting spl files...");
		System.out.println(dir.getCanonicalPath());
		// data file
		File dataFile = new File(dir, dataFileName);
		File dstDir = new File(dir, "splZipFiles");
		if (dstDir.exists() == false) {
			dstDir.mkdirs();
		}
		// extract original zip file
		ZipUtil.unzip(dataFile, dstDir, true);
		// get extracted files
		File[] files = dstDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String srcFileName = file.getName().toLowerCase();
			if(srcFileName.endsWith(".zip") == false) {
				// skip anything that is not a zip file (e.g. .svn files)
				continue;
			}
			String dstFileName = srcFileName.substring(0, srcFileName
					.lastIndexOf(".zip"));
			File finalDstDir = new File(dir, "splXml/" + dstFileName);
			if (finalDstDir.exists() == false) {
				finalDstDir.mkdirs();
			}
			ZipUtil.unzip(file, finalDstDir);
		}
		return new File(dir, "splXml");
	}

}
