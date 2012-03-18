/**
 * 
 */
package ransac;


import java.util.List;
import java.util.Random;

import basic.ImageData;


/**
 * @author Agnieszka Glabala
 *
 */
public class Ransac {
	private ImageData q;
	private ImageData d;
	
	public Ransac(ImageData q, ImageData d) {
		this.q = q;
		this.d = d;
	}
	
	
	
	/**
	 * 
	 * @param pairs - indeksy znalezionych par
	 * @param i - query obraz
	 * @param j - data obraz
	 * @return
	 */
	public Model getBestModel(List<int[]> pairs) {
		Random r = new Random();
		int i1=0, i2=0, i3=0; //indeksy trzech par
		Model bestModel = null;
		Model model=null;
		//szukamy modelu który ma wystarczająco dużo inlierów
		for (int it = 0; it < Main.iters; it++) {
			model = null;
			while(model==null) {
				i1 = r.nextInt(pairs.size());
				i2 = r.nextInt(pairs.size());
				i3 = r.nextInt(pairs.size());
				//System.out.println(i1 + " " + i2 + " " + i3);
				model = Model.evaluateModel(q.params[pairs.get(i1)[0]], q.params[pairs.get(i2)[0]], q.params[pairs.get(i3)[0]], 
									  		d.params[pairs.get(i1)[1]], d.params[pairs.get(i2)[1]], d.params[pairs.get(i3)[1]]); 				
			}
			model.evaluateInliers(pairs, q.params, d.params, Main.eps);
			//System.out.println(model.getModel()[0] + " " + model.getModel()[1] + " " + model.getModel()[2] + " " + model.getInliers().size());
			if(bestModel==null) bestModel=model;
			else {
				if(model.countInliers()>bestModel.countInliers()) {
					bestModel = model;
				}
			}
		}
		return bestModel;
	}
}
