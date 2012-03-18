package ransac;

import java.util.ArrayList;
import java.util.List;

public class Model {
	private float[] model = null;
	private List<int[]> inliers=new ArrayList<int[]>();
	private float[][] randomPoints; //[q1,q2,q3,d1,d2,d3]
	Model(float[] model, float[][] rP){
		randomPoints = rP;
		this.model = model;
	}

	public static double dist(float[] v, float[] u) {
		double dist=0;
		for (int i = 0; i < u.length; i++) {
			dist+=(v[i]-u[i])*(v[i]-u[i]);
		}
		return Math.sqrt(dist);
	}
	
	/**
	 * Wejście to pary punktów (p1, q1) (p2, q2), (p3,q3) p - punkt z query, q - z data
	 * @param q1
	 * @param q2
	 * @param q3
	 * @param d1
	 * @param d2
	 * @param d3
	 * @return model [a,b,c,d,e,f]
	 */
	 public static Model evaluateModel(float[] q1, float[] q2, float[] q3, float[] d1, float[] d2, float[] d3) {
		//sprawdzenie czy punkty nie są współliniowe
		 boolean print = false;
		if((dist(q1,q2)+dist(q2,q3))/dist(q3,q1)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if((dist(q1,q3)+dist(q3,q2))/dist(q1,q2)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if((dist(q2,q1)+dist(q1,q3))/dist(q2,q3)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if((dist(d1,d2)+dist(d2,d3))/dist(d3,d1)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if((dist(d1,d3)+dist(d3,d2))/dist(d1,d2)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if((dist(d2,d1)+dist(d1,d3))/dist(d2,d3)<Main.min) {
			if(print) System.out.println("1: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		//sprawdzenie czy odległości pomiędzy punktami nie są za mała lub za duże
		if(dist(q1,q2)>Main.maxCoverage || dist(q1,q2)<Main.minCoverage) {
			if(print) System.out.println("2q: "+dist(d1,d2)+" "+dist(d2,d3)+" "+dist(d3,d1));
			return null;
		}
		if(dist(q3,q2)>Main.maxCoverage || dist(q3,q2)<Main.minCoverage) {
			if(print) System.out.println("2w: "+dist(d1,d2)+" "+dist(d2,d3)+" "+dist(d3,d1));
			return null;
		}
		if(dist(q1,q3)>Main.maxCoverage || dist(q1,q3)<Main.minCoverage) {
			if(print) System.out.println("2e: "+dist(d1,d2)+" "+dist(d2,d3)+" "+dist(d3,d1));
			return null;
		}
		if(dist(d1,d2)>Main.maxCoverage || dist(d1,d2)<Main.minCoverage) {
			if(print) System.out.println("2r: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if(dist(d3,d2)>Main.maxCoverage || dist(d3,d2)<Main.minCoverage) {
			if(print) System.out.println("2t: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		if(dist(d1,d3)>Main.maxCoverage || dist(d1,d3)<Main.minCoverage) {
			if(print) System.out.println("2y: "+dist(q1,q2)+" "+dist(q2,q3)+" "+dist(q3,q1));
			return null;
		}
		
		float[][] randomPoints = new float[][] {q1,q2,q3,d1,d2,d3};
		
		float u1=d1[0];
		float u2=d2[0];
		float u3=d3[0];
		float v1=d1[1];
		float v2=d2[1];
		float v3=d3[1];
		
		float x1=q1[0];
		float x2=q2[0];
		float x3=q3[0];
		float y1=q1[1];
		float y2=q2[1];
		float y3=q3[1];
		
		final float dx12 = x1 - x2;
		final float dx13 = x1 - x3;
		final float dy12 = y1 - y2;
		final float dy13 = y1 - y3;
		
		final float du12 = u1 - u2;
		final float du13 = u1 - u3;
		final float dv12 = v1 - v2;
		final float dv13 = v1 - v3;
		
		final float m = dx12 * dy13 - dx13 * dy12;
		if (m == 0) {
			return null;
		}
		
		float a = (du12 * dy13 - du13 * dy12) / m;
		float d = (dv12 * dy13 - dv13 * dy12) / m;
		
		float b = (du13 * dx12 - du12 * dx13) / m;
		float e = (dv13 * dx12 - dv12 * dx13) / m;
		
		float c = u1 - a * x1 - b * y1;
		float f = v1 - d * x1 - e * y1;
		
		
		/*
		float tmp=(y1-y3)/(y1-y2);
		float a = (u1-u3+tmp*(u1-u3))/
				  (x1-x3-tmp*(x1-x2));
		float b = (u1-u2-a*(x1-x2))/(y1-y2);
		float c = u1-a*x1-b*y1;
		float d = (v1-v3+tmp*(v1-u3))/
				  (x1-x3-tmp*(x1-x2));
		float e = (v1-v2-d*(x1-x2))/(y1-y2);
		float f = v1-d*x1-e*y1;
		*/
		
		/*
		float a =
			(q1[0]*p2[1] - q2[0]*p1[1] - q1[0]*p3[1] + q3[0]*p1[1] + q2[0]*p3[1] - q3[0]*p2[1])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);
			 
		float b =
			-(q1[0]*p2[0] - q2[0]*p1[0] - q1[0]*p3[0] + q3[0]*p1[0] + q2[0]*p3[0] - q3[0]*p2[0])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);
			 
		float c =
			(q1[0]*p2[0]*p3[1] - q1[0]*p3[0]*p2[1] - q2[0]*p1[0]*p3[1] + q2[0]*p3[0]*p1[1] + q3[0]*p1[0]*p2[1] - q3[0]*p2[0]*p1[1])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);

		float d =
			(q1[1]*p2[1] - q2[1]*p1[1] - q1[1]*p3[1] + q3[1]*p1[1] + q2[1]*p3[1] - q3[1]*p2[1])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);
			 
			 
		float e =
			-(q1[1]*p2[0] - q2[1]*p1[0] - q1[1]*p3[0] + q3[1]*p1[0] + q2[1]*p3[0] - q3[1]*p2[0])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);
			 
		float f =
			(q1[1]*p2[0]*p3[1] - q1[1]*p3[0]*p2[1] - q2[1]*p1[0]*p3[1] + q2[1]*p3[0]*p1[1] + q3[1]*p1[0]*p2[1] - q3[1]*p2[0]*p1[1])/(p1[0]*p2[1] - p2[0]*p1[1] - p1[0]*p3[1] + p3[0]*p1[1] + p2[0]*p3[1] - p3[0]*p2[1]);
			
		*/
		return new Model(new float[] {a,b,c,d,e,f}, randomPoints);
	}
	
	public void evaluateInliers(List<int[]> pairs, float[][] paramsQ, float[][] paramsD, double eps) {
		inliers = new ArrayList<int[]>();
		double d;
		for (int i = 0; i < pairs.size(); i++) {
			d=distModel(paramsQ[pairs.get(i)[0]], paramsD[pairs.get(i)[1]]);
			//System.out.println("Element " + i + " => " + d);
			if(d<eps) {
				inliers.add(pairs.get(i));
			}
		}
	}
	int countInliers() {
		 return inliers.size();
	}
	public List<int[]> getInliers() {
		return inliers;
	}
	public float[][] getPoints() {
		return randomPoints;
	}
	float[] getModel() {
		return model;
	}
	 /**
		 * odległość (ax + by + c, dx + ey + f ) od punktu (u, v )
		 * @param model (a,b,c,d,e,f)
		 * @param query (x,y)
		 * @param data (u,v)
		 * @return
		 */
		private double distModel(float[] query, float[] data) {
			return Math.sqrt(Math.pow(model[0]*query[0] + model[1]*query[1] + model[2]-data[0], 2)+Math.pow(model[3]*query[0] + model[4]*query[1] + model[5]-data[1], 2));
		}
}
