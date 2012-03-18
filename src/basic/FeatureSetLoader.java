package basic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



/**
 * Generic feature loader.
 * Makes feature loading very easy.
 * 
 * @author Mariusz Paradowski
 */
public class FeatureSetLoader {
	private String featureCode;
	
	public static String SURF = "Surf";
	public static String MSERSURF = "MserSurf";
	public static String HESAFFSIFT = "HesAffSift";
	public static String HARAFFSIFT = "HarAffSift";
	public static String MSERSIFT = "MserSift";
	public static String MSERRGBSIFT = "MserRGBSift";
	public static String MSERRGBSIFTNA = "MserRGBSiftNoAngle";
	public static String HESLAPSIFT = "HesLapSift";
	//public static String HESAFFGLOH = "HesAffGloh";
	public static String HARAFFGLOH = "HarAffGloh";
	public static String HARAFFMOM = "HarAffMom";
	public static String MSERINV   = "MserInv";
	
	/**
	 * For the moment cache is turned off.
	 * Maybe someone will have better idea for the cache
	 */
	public static boolean USECACHE = false;
	
	private Map<File, IFeature[]> cachedFeatures;
	
	private String appendExtension = "";
	private String ext;
	/**
	 * Constructor with the given feature code.
	 * ext - dodatkowe rozszerzenie do filtrowanych siftów
	 */
	public FeatureSetLoader(String featureCode, String ext) {
		this.featureCode = featureCode;
		this.cachedFeatures = new HashMap<File, IFeature[]>();
		this.ext = ext;
	}
	
	public FeatureSetLoader(String featureCode, String appendExtension, String ext) {
		this.featureCode = featureCode;
		this.cachedFeatures = new HashMap<File, IFeature[]>();
		this.appendExtension = appendExtension;
		this.ext = ext;
	}
	
	/**
	 * Reads an array of features for the given image name
	 */
	public IFeature[] getFeatures(File file) {
		IFeature[] features = null;
		if (USECACHE) {
			//cache is not possible, coordinates are modified
			if (this.cachedFeatures.containsKey(file)) {
				return this.cachedFeatures.get(file);
			}
		}
		if (SURF.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".surf"+ext + this.appendExtension)};
		}
		if (MSERSURF.equals(this.featureCode)) {
			IFeature featR1 = new ImageFromMikolajczykFile(file, ".mserR.surf"+ext + this.appendExtension);
			IFeature featG1 = new ImageFromMikolajczykFile(file, ".mserG.surf"+ext + this.appendExtension);
			IFeature featB1 = new ImageFromMikolajczykFile(file, ".mserB.surf"+ext + this.appendExtension);
			features = new IFeature[]{featR1, featG1, featB1};
		}
		if (MSERRGBSIFT.equals(this.featureCode)) {
			IFeature featR1 = new ImageFromMikolajczykFile(file, ".mserR.sift"+ext + this.appendExtension);
			IFeature featG1 = new ImageFromMikolajczykFile(file, ".mserG.sift"+ext + this.appendExtension);
			IFeature featB1 = new ImageFromMikolajczykFile(file, ".mserB.sift"+ext + this.appendExtension);
			features = new IFeature[]{featR1, featG1, featB1};
		}
		if (MSERRGBSIFTNA.equals(this.featureCode)) {
			IFeature featR1 = new ImageFromMikolajczykFile(file, ".mserR.siftN"+ext + this.appendExtension);
			IFeature featG1 = new ImageFromMikolajczykFile(file, ".mserG.siftN"+ext + this.appendExtension);
			IFeature featB1 = new ImageFromMikolajczykFile(file, ".mserB.siftN"+ext + this.appendExtension);
			features = new IFeature[]{featR1, featG1, featB1};
		}
		if (MSERSIFT.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".mser.sift"+ext + this.appendExtension)};
		}
		if (MSERINV.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".mser.inv"+ext + this.appendExtension)};
		}
		if (HESAFFSIFT.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".hesaff.sift"+ext + this.appendExtension)};
			if (features[0].getParams() == null) { return null; }
		}
		if (HARAFFSIFT.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".haraff.sift"+ext + this.appendExtension)};
			if (features[0].getParams() == null) { return null; }
		}
		if (HESLAPSIFT.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".heslap.sift"+ext + this.appendExtension)};
		}
		if (HARAFFMOM.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromMikolajczykFile(file, ".haraff.mom"+ext + this.appendExtension)};
		}
		/*if (HARAFFGLOH.equals(this.featureCode)) {
			features = new IFeature[]{new ImageFromCopiesMikolajczykFile(file, ".haraff.gloh" + this.appendExtension)};
		}*/
		if (features == null) {
			//System.out.println("Reading non-standard feature type: " + this.featureCode);
			File featureFile = new File(file.getPath() + this.featureCode);
			if (featureFile.exists()) {
				features = new IFeature[]{new ImageFromMikolajczykFile(file, this.featureCode)};
			}
		}
		if (USECACHE) {
			this.cachedFeatures.put(file, features);
		}
		return features;
	}
	
	/**
	 * Gets the feature code
	 */
	public String getCode() {
		return this.featureCode;
	}
}
