package basic;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import ransac.Main;
import ransac.Model;

public class ImageData {
	
	public float[][] params;  //punkt -> param
	public float[][] values;  //punkt -> sift
	public int[] clusters; //punkt -> klaster
	public short[][] neighs; //punkt -> 60 sąsiadów
	public List<Integer>[] clusterpts;//List[cluster] -> set of points
	public File f;
	public BufferedImage img;
	public int[] bow;
	public ImageData(float[][] params, List<Integer>[] clusterpts, File f) throws IOException {
		this.f = f;
		img = ImageIO.read(f);
		float w = img.getWidth();
		float h = img.getHeight();
		this.params = new float[params.length][];
		for (int i = 0; i < params.length; i++) {
			this.params[i] = new float[] {params[i][0]/w, params[i][1]/h};
		}
		
		this.clusterpts = clusterpts;
	}
	public ImageData(float[][] params, float[][] values, List<Integer>[] clusterpts, File f) throws IOException {
		this.f = f;
		img = ImageIO.read(f);
		float w = img.getWidth();
		float h = img.getHeight();
		this.params = new float[params.length][];
		for (int i = 0; i < params.length; i++) {
			this.params[i] = new float[] {params[i][0]/w, params[i][1]/h};
		}
		this.values = values;
		this.clusterpts = clusterpts;
	}
	public ImageData(float[][] params, float[][] values, int[] clusters, short[][] neighs, List<Integer>[] clusterpts, int[] bow, File f) throws IOException {
		this.f = f;
		img = ImageIO.read(f);
		float w = img.getWidth();
		float h = img.getHeight();
		this.params = new float[params.length][];
		for (int i = 0; i < params.length; i++) {
			this.params[i] = new float[] {params[i][0]/w, params[i][1]/h};
		}
		this.values = values;
		this.clusters = clusters;
		this.clusterpts = clusterpts;
		this.neighs = neighs;
		this.bow = bow;
	}
	public List<int[]> generatePairsFast(ImageData dat) {
		List<int[]> pairs = new ArrayList<int[]>(); //[punkt query, punkt data] 
		List<Integer> query; 
		List<Integer> data; 
		//dla każdego klastra
		for (int k = 0; k < Main.clNum; k++) {
			//punkty dla dwóch obrazów o tym numerze klastra
			query = clusterpts[k];
			data = dat.clusterpts[k];
			//po punktach z zapytania
			if(query==null || query.size()==0 || data==null || data.size()==0) {
				continue;
			}

			if (query.size() > 40) {
				continue;
			}
			if (data.size() > 40) {
				continue;
			}
			
			for (int k2 = 0; k2 < query.size(); k2++) {
				for (int l = 0; l < data.size(); l++) {
					pairs.add(new int[] {query.get(k2), data.get(l)});
				}
			}
			
		}
		return pairs;
	}
	public List<int[]> generatePairs(ImageData dat) {
		List<int[]> pairs = new ArrayList<int[]>(); //[punkt query, punkt data] 

		for (int i = 0; i < clusters.length; i++) {
			for (int j = 0; j < dat.clusters.length; j++) {
				//if(clusters[i]==dat.clusters[j] && bow[i]<40 && dat.bow[j]<40) pairs.add(new int[] {i,j});
				if(clusters[i]==dat.clusters[j]) pairs.add(new int[] {i,j});
			}
		}

		return pairs;
	}
	
	public List<int[]> neighBasedPairs(ImageData dat) {
		List<int[]> resList = new ArrayList<int[]>();
		int maxPoints=10;
		//cl - tablica zawierająca pierwsze 10 punktów zawartych w danym klastrze
		int[][] clq = new int[Main.clNum][maxPoints];
		int[][] cld = new int[Main.clNum][maxPoints];
		//il - bow
		int[] ilq = new int[Main.clNum];
		int[] ild = new int[Main.clNum];
		//tworzenie tablic dla obrazu-zapytania (query) i obrazu z którym go porównujemy (data) 
		for (int i = 0; i < clusters.length; i++) {
			if (ilq[clusters[i]] < maxPoints) {
				int clust = clusters[i];
				int index = ilq[clust];
				clq[clust][index] = i;
				ilq[clust]++;
			}
		}
		for (int i = 0; i < dat.clusters.length; i++) {
			if (ild[dat.clusters[i]] < maxPoints) {
				cld[dat.clusters[i]][ild[dat.clusters[i]]] = i;
				ild[dat.clusters[i]]++;
			}
		}

		int[] qnnc = new int[Main.clNum];
		
		List<Ordered<int[]>>[] pairs = new ArrayList[Main.clNum];
		
		for (int i = 0; i < Main.clNum; i++) {
			//dla każdego klastra
			for (int j = 0; j < ilq[i]; j++) {
				//dla kazdego punktu z danego klastra z zapytania
				int qp = clq[i][j]; //index tego punktu
				for (int nq = 0; nq < neighs[qp].length; nq++) {
					//dla wszystkich sasiadów tego punktu z zapytania
					int qcl = clusters[neighs[qp][nq]];
					qnnc[qcl]++; //tworze BOW sasiedztwa dla punktu w zapytaniu 
				}
				for (int k = 0; k < ild[i]; k++) {
					//dla każdego punktu z danego klastra z data
					int dp = cld[i][k];
					int cnt = 0; //liczba pasujacych sasiadow w rozwazanej parze
					for (int nd = 0; nd < dat.neighs[dp].length; nd++) {
						//dla wszystkich sasiadów danego punktu w data
						int dcl = dat.clusters[dat.neighs[dp][nd]]; //klaster tego punktu
						cnt += qnnc[dcl]; //zliczam pasujacych sasiadow
						//otrzymujemy przeciecie dwoch histogramow
					}
					/* v2
					 */
					if(cnt>0) {
						if(pairs[i]==null) {
							pairs[i] = new ArrayList<Ordered<int[]>>();
						}
						pairs[i].add(new Ordered<int[]>(new int[] {qp, dp}, cnt));
					}
					/*
					*/
					
					/*
					 * v1
					if (cnt > clam[i]) {
						clam[i] = cnt; //najlepszy znaleziony (najwieksze sasiedztwo)
						claq[i] = qp; // do klastra przypisuje punkt
						clad[i] = dp; // do klastra przypisuje punkt
					}
					*/
				}
				for (int nq = 0; nq < neighs[qp].length; nq++) {
					qnnc[clusters[neighs[qp][nq]]]--; //zeruje BOW sasiedztwa
				}
			}
		}
		
		/*
		 * v2
		 */
		for (int i = 0; i < pairs.length; i++) {
			if(pairs[i]!=null) {
				Collections.sort(pairs[i]);
				for (int j = Math.min(pairs[i].size(),Main.nElem)-1; j >= 0; j--) {
					resList.add(pairs[i].get(j).get());
				}
			}
		}
		/*
		 * 
		 */
		
		/*
		 * v1
		for (int i = 0; i < claq.length; i++) {
			if(claq[i]>0 && clad[i]>0) {
				resList.add(new int[] {claq[i], clad[i]});
			}
		}
		*/
		return resList;
	}
	
	public List<int[]> CoherentPairs(ImageData dat) {
		float[][] values1 = this.values;
		float[][] values2 = dat.values;
		
		List<int[]> pairs = new ArrayList<int[]>();
		int[] closest1 = new int[values1.length];
		int[] closest2 = new int[values2.length];
		boolean[] found2 = new boolean[values2.length];
		
		for (int i = 0; i < values1.length; i++) {
			closest1[i] = -1;
			double minD = Double.MAX_VALUE;
			for (int j = 0; j < values2.length; j++) {
				double d = dist(values1[i], values2[j]);
				if (d < minD) {
					minD = d;
					closest1[i] = j;
				}
			}
			if (closest1[i] == -1) {
				System.err.println("Coherent pairs error. Please check the data.");
				return pairs;
			}
			found2[closest1[i]] = true;
		}
		
		for (int i = 0; i < values2.length; i++) {
			if (found2[i]) {
				closest2[i] = -1;
				double minD = Double.MAX_VALUE;
				for (int j = 0; j < values1.length; j++) {
					double d = dist(values2[i], values1[j]);
					if (d < minD) {
						minD = d;
						closest2[i] = j;
					}
				}
				//closest2[i] = this.nn.nn(values1, current);
			} else {
				closest2[i] = -1;
			}
		}
		
		for (int i = 0; i < values1.length; i++) {
			int found    = closest1[i];
			int feedback = closest2[found];
			if (feedback == i) {
				int[] pair = null;
				pair = new int[]{i, found};
				pairs.add(pair);
			}
		}
		
		return pairs;
	}
	
	public List<int[]> generateAllPairs(ImageData dat) {
		List<int[]> pairs = new ArrayList<int[]>(); //[punkt query, punkt data] 

		for (int k = 0; k < values.length; k++) {
			//dla każdego punktu z query
			double d;
			double mindist=Double.MAX_VALUE;
			int dataNN=-1;
			//po punktach z zapytania
			for (int k2 = 0; k2 < dat.values.length; k2++) {
				d= dist(values[k], dat.values[k2]);
				if(d<mindist) {
					mindist=d;
					dataNN=k2;
				}
			}
			boolean found=false;
			for (int l = 0; l < values.length; l++) {
				d=dist(dat.values[dataNN], values[l]);
				if(d<mindist) {
					found=true;
					break;
				}
			}
			if(!found) {
				pairs.add(new int[] {k, dataNN});
			}
		}
		return pairs;
	}
	/**
	 * 
	 * @param i - index of query file
	 * @param j - index of data file
	 * @param query - pairs of query
	 * @param data - pairs of data
	 * @throws IOException 
	 */
	public void drawPairs(ImageData d, List<int[]> pairs, File outF, Model m) throws IOException {
		BufferedImage out = new BufferedImage(Math.max(img.getWidth(), d.img.getWidth()), img.getHeight()+ d.img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = out.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.drawImage(d.img, 0, img.getHeight(), null);
		float[][] triangles = m.getPoints();//[q1,q2,q3,d1,d2,d3]
		for(int[] p : pairs) {
				g.drawLine((int)(params[p[0]][0]*img.getWidth()), (int)(params[p[0]][1]*img.getHeight()), 
					   (int)(d.params[p[1]][0]*d.img.getWidth()), img.getHeight()+ (int)(d.params[p[1]][1]*d.img.getHeight()));
		}
		g.setColor(new Color(0x00ff00));
		g.drawLine((int)(triangles[0][0]*img.getWidth()), (int)(triangles[0][1]*img.getHeight()), (int)(triangles[2][0]*img.getWidth()), (int)(triangles[2][1]*img.getHeight()));
		g.drawLine((int)(triangles[0][0]*img.getWidth()), (int)(triangles[0][1]*img.getHeight()), (int)(triangles[1][0]*img.getWidth()), (int)(triangles[1][1]*img.getHeight()));
		g.drawLine((int)(triangles[1][0]*img.getWidth()), (int)(triangles[1][1]*img.getHeight()), (int)(triangles[2][0]*img.getWidth()), (int)(triangles[2][1]*img.getHeight()));
		
		g.drawLine((int)(triangles[3][0]*d.img.getWidth()), (int)(triangles[3][1]*d.img.getHeight()+img.getHeight()), (int)(triangles[5][0]*d.img.getWidth()), (int)(triangles[5][1]*d.img.getHeight()+img.getHeight()));
		g.drawLine((int)(triangles[3][0]*d.img.getWidth()), (int)(triangles[3][1]*d.img.getHeight()+img.getHeight()), (int)(triangles[4][0]*d.img.getWidth()), (int)(triangles[4][1]*d.img.getHeight()+img.getHeight()));
		g.drawLine((int)(triangles[4][0]*d.img.getWidth()), (int)(triangles[4][1]*d.img.getHeight()+img.getHeight()), (int)(triangles[5][0]*d.img.getWidth()), (int)(triangles[5][1]*d.img.getHeight()+img.getHeight()));

		ImageIO.write(out, "png", outF);
	}/**
	 * 
	 * @param i - index of query file
	 * @param j - index of data file
	 * @param query - pairs of query
	 * @param data - pairs of data
	 * @throws IOException 
	 */
	public void drawPairs(ImageData d, List<int[]> pairs, File outF) throws IOException {
		BufferedImage out = new BufferedImage(Math.max(img.getWidth(), d.img.getWidth()), img.getHeight()+ d.img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = out.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.drawImage(d.img, 0, img.getHeight(), null);
		for(int[] p : pairs) {
				g.drawLine((int)(params[p[0]][0]*img.getWidth()), (int)(params[p[0]][1]*img.getHeight()), 
					   (int)(d.params[p[1]][0]*d.img.getWidth()), img.getHeight()+ (int)(d.params[p[1]][1]*d.img.getHeight()));
		}
		ImageIO.write(out, "png", outF);
	}
	static double dist(float[] v, float[] u) {
		double dist=0;
		for (int i = 0; i < u.length; i++) {
			dist+=(v[i]-u[i])*(v[i]-u[i]);
		}
		return Math.sqrt(dist);
	}
}
