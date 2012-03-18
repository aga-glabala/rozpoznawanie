package basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import run.Paths;

/**
 * Image file loaded according to Mikolajczyk format.
 * Probably the most useful and commonly used loader within the library
 * 
 * @author Mariusz Paradowski
 */
public class ImageFromMikolajczykFile implements IFeature {
	
	private static String EXTENSION = ".haraff.sift";
	
	//private static String EXTENSION = ".blk128.xyrgb";
	//private static String EXTENSION = ".sblock.mean";
	//private static String EXTENSION = ".superpix.xyrgb";
	//private static String EXTENSION = ".superpix.xylong";
	//private static String EXTENSION = ".enriched.circle";
	
	private float[][] values;
	private float[][] params;
	private File imageFile;
	
	/**
	 * Constructor with a customized extension
	 */
	public ImageFromMikolajczykFile(File image, String extension) {
		this.imageFile = image;
		File featureFile;
		String ext =extension.split("\\.")[extension.split("\\.").length-1]; 
		if(ext.equals("neighs"))
		{
			featureFile = new File(Paths.nPath+image.getName() + extension);
		}
		else
		{
			featureFile = new File(Paths.fPath+image.getName() + extension);
		} 
		this.read(featureFile);
	}
	
	/**
	 * Constructor with a default extension
	 */
	public ImageFromMikolajczykFile(File image) {
		this.imageFile = image;
		File featureFile = new File(image.getPath() + EXTENSION);
		this.read(featureFile);
	}
	
	/**
	 * Reads and parses the feature file
	 */
	private void read(File featureFile) {
		List<float[]> features = new ArrayList<float[]>();
		List<float[]> params = new ArrayList<float[]>();
		try {
	        BufferedReader in = new BufferedReader(new FileReader(featureFile));
	        String str;
	        while ((str = in.readLine()) != null) {
	        	String [] sub = str.split(" ");
	        	if (sub.length < 5) {
	        		continue;
	        	}
	        	float[] param = new float[5];
	        	param[0] = Float.parseFloat(sub[0]);
	        	param[1] = Float.parseFloat(sub[1]);
	        	
	        	param[2] = Float.parseFloat(sub[2]);
	        	param[3] = Float.parseFloat(sub[3]);
	        	param[4] = Float.parseFloat(sub[4]);
	        		    	    	    	    
	    	    float [] desc = new float[sub.length - 5];
	    	    for (int i = 0; i < desc.length; i++) {
	    	    	desc[i] = Float.parseFloat(sub[i + 5]);
	    	    }
	    	    
	    	    params.add(param);
	    	    features.add(desc);
	        }
	        in.close();
	    } catch (Exception e) {
	    	System.err.println("ERROR! Problem reading file: " + featureFile.getPath());
	    	System.err.println("Program will most probably terminate!");
	    	return;
	    }
	    
	    this.values = features.toArray(new float[0][]);
	    this.params = params.toArray(new float[0][]);
	}

	@Override
	/**
	 * Gets the descriptor values
	 */
	public float[][] getValues() {
		return this.values;
	}
	
	@Override
	/**
	 * Gets the image name
	 */
	public File getImageFile() {
		return this.imageFile;
	}

	@Override
	/**
	 * Gets the parameter values
	 */
	public float[][] getParams() {
		return this.params;
	}
}
