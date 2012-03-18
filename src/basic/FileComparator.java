package basic;

import java.io.File;
import java.util.Comparator;

public class FileComparator implements Comparator<File> {
	@Override
	public int compare(File arg0, File arg1) {
		return arg0.getName().compareTo(arg1.getName());
	}
}
