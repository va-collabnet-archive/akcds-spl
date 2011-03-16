package gov.va.akcds.util.fileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class FileUtil {

	public static String getAsString(File src) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(src));
		StringBuffer sb = new StringBuffer();
		for (String str = reader.readLine(); str != null; str = reader.readLine()) {
			sb.append(str);
			sb.append("\n");
		}
		return new String(sb);
	}
	
}
