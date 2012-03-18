package basic;

import java.util.List;

/**
 * Clustering algorithm interface.
 * 
 * @author Mariusz Paradowski
 */
public interface IClusterer {
	/**
	 * Perform clustering of a feature set. Return cluster assignment.
	 */
	public int[] cluster(List<float[]> features);
}
