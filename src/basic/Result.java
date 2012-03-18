package basic;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class Result {
	public List<Ordered<ImageData>> files;
	int order = 1;
	public ImageData ffor;
	public int method;
	
	/**
	 * Zapamiętuje wyniki i je sortuje
	 * @param files
	 */
	public Result(List<Ordered<ImageData>> files, ImageData ffor, int method) {
		this.files = files;
		Collections.sort(this.files);
		this.ffor = ffor;
	}
	public Result(List<Ordered<ImageData>> files, int order, ImageData ffor, int method) {
		this.files = files;
		Collections.sort(this.files);
		this.order = order;
		this.ffor = ffor;
	}
	
	/**
	 * Daje cut plików z największymi watrościami
	 * @param cut
	 * @return
	 */
	public ImageData[] getFiles(int cut) {
		ImageData[] fs = new ImageData[Math.min(files.size(), cut)];
		for (int i = 0; i < Math.min(files.size(), cut); i++) {
			fs[i] = files.get(order<0 ? Math.min(files.size(), cut)-1-i : i).get();          
		}
		return fs;
	}
}
