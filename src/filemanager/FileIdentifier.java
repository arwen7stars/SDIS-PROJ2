package filemanager;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class FileIdentifier {
	
	private String identifier;
	
	public FileIdentifier(String filePath) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			FileInputStream fis = new FileInputStream(filePath);

			byte[] dataBytes = new byte[8192];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = md.digest();
			fis.close();

			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			this.identifier = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		return this.identifier;
	}
}
