package basic;

import java.io.File;

/**
 * Main feature file interface. Should be used for feature representation.
 * 
 * @author Mariusz Paradowski
 */
public interface IFeature {
	
	/**
	 * Name of the image file
	 */
	File getImageFile();
	
	/**
	 * Feature vector values (descriptor)
	 */
	float [][] getValues();
	
	/**
	 * Feature parameters (detector)
	 */
	float [][] getParams();	
}
