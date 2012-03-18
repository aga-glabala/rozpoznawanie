package basic;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Stores the feature data in a file according to Mikolajczyk format.
 * 
 * @author Mariusz Paradowski
 */
public class SaveMikolajczykFromFeature {
		
	/**
	 * Save the features. Float feature values.
	 * In case of an error, the application is terminated.
	 */
	public static void save(File file, IFeature features) {
		try {
			FileWriter outFile = new FileWriter(file);
			PrintWriter out = new PrintWriter(outFile);
			
			float[][] params = features.getParams();
			float[][] values = features.getValues();
			
			out.println(values[0].length);
			out.println(params.length);
			
			for (int i = 0; i < params.length; i++) {
				float[] param = params[i];
				float[] value = values[i];
				out.print(param[0] + " ");
				out.print(param[1] + " ");
				out.print(param[2] + " ");
				out.print(param[3] + " ");
				out.print(param[4] + " ");
				
				for (int j = 0; j < value.length; j++) {
					out.print(value[j] + " ");
				}
				out.println();
			}
			out.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	System.exit(0);
	    }
	}
	
	/**
	 * Save the features. Integer feature values.
	 */
	public static boolean saveFeaturesInteger(File file, IFeature features) {
		try {
			FileWriter outFile = new FileWriter(file);
			PrintWriter out = new PrintWriter(outFile);
			
			float[][] params = features.getParams();
			float[][] values = features.getValues();
			
			out.println(values[0].length);
			out.println(params.length);
			
			for (int i = 0; i < params.length; i++) {
				float[] param = params[i];
				float[] value = values[i];
				out.print(param[0] + " ");
				out.print(param[1] + " ");
				out.print(param[2] + " ");
				out.print(param[3] + " ");
				out.print(param[4] + " ");
				
				for (int j = 0; j < value.length; j++) {
					out.print((int)value[j] + " ");
				}
				out.println();
			}
			out.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return false;
	    }
	    return true;
	}
}
