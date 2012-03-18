package basic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Returns the substructure of a folder.
 * 
 * @author Mariusz Paradowski
 */
public class FileStructure {
	
	/**
	 * Reads the folder and file structure
	 */
	public static List<File> read(File root) {
		List<File> result = new ArrayList<File>();
		if (root.isDirectory()) {
			File [] files = root.listFiles();
			for (File file : files) {
				result.addAll(read(file));
			}
		} else {
			result.add(root);
		}
		return result;
	}
	
	public static File[] readFolderSorted(File folder) {
		File [] files = folder.listFiles();
		Arrays.sort(files, new FileComparator());
		return files;
	}
}
