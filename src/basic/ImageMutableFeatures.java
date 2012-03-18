package basic;


import java.io.File;

/**
 * Mutable feature set. Used primarily for the storage purpose
 * 
 * @author Mariusz Paradowski
 */
public class ImageMutableFeatures implements IFeature {
	
	private float[][] params;
	private float[][] values;
	
	public ImageMutableFeatures(float[][] params, float[][] values) {
		this.params = params;
		this.values = values;
	}

	@Override
	/**
	 * Name is not yet given. We need to store the file first.
	 */
	public File getImageFile() {
		return null;
	}

	@Override
	public float[][] getParams() {
		return this.params;
	}

	@Override
	public float[][] getValues() {
		return this.values;
	}
}
