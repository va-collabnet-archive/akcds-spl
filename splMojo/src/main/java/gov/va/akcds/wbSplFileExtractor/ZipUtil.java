package gov.va.akcds.wbSplFileExtractor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

	public static final int BUFFER = 2048;

	public static void unzip(File zipFile, File dstDir) throws Exception {
		unzip(zipFile, dstDir, false);
	}

	public static void unzip(File zipFile, File dstDir, boolean echo)
			throws Exception {
		ZipInputStream zis = null;
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(zipFile);
			zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			int cnt = 0;
			while ((entry = zis.getNextEntry()) != null) {
				cnt++;
				if (echo == true) {
					if(cnt % 10 == 0) {
						System.out.print(".");
					}
					if(cnt % 500 == 0) {
						System.out.println("");
					}
				}
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new FileOutputStream(new File(dstDir,
						entry.getName()));
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
		} finally {
			if (zis != null) {
				zis.close();
			}
		}
	}

}
