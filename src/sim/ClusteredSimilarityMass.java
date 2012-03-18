package sim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.Paths;

import basic.Chart;
import basic.FeatureSetLoader;
import basic.IFeature;
import basic.Ordered;
import basic.Results;


public class ClusteredSimilarityMass{

	//PARAMETRY
	public int clNum = 15000;
	public boolean unique = false;
	public boolean ellipse = false;
	public int elements = 40;
	public int firstN = 5;
	public boolean cntpp = false;
	
	private String db;
	private FeatureSetLoader lf;
	private FeatureSetLoader ln;
	private long time=0;
	private FloatKMeans cluster;
	
	private String name; //nazwa filtrowania
	
	private List<File> content = new ArrayList<File>();
	private Map<File, int[]> cFeatures = new HashMap<File, int[]>();
	private Map<File, float[][]> cParams = new HashMap<File, float[][]>();
	private Map<File, short[][]> cNeighs = new HashMap<File, short[][]>();
	public boolean filtering=false; //czy sifty były filtrowane
	double part = 0.1;
	public List<int[]> pairs;
	public ClusteredSimilarityMass(String db, String name)
	{
		this.db = db;
		this.name = name;
		if(name.equals(""))
			name="";
		else
			name="."+name;
		lf = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT,name,db);
		ln = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, unique ? ".unique.neighs" : ellipse ? ".ellipse.neighs" : ".neighs",name);
		pairs = new ArrayList<int[]>();
	
	}
	private boolean storeFeatures(File file) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			out.println(this.content.size());
			for (File img : this.cFeatures.keySet()) {
				out.print(img.getPath() + " ");
				int[] cFeat = this.cFeatures.get(img);
				for (int i = 0; i < cFeat.length; i++) {
					out.print(cFeat[i] + " ");
				}
				out.println();
			}			
			out.flush();
			out.close();
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}
	
	private boolean readFeatures(File file) {
		BufferedReader in = null;
		try {
	        in = new BufferedReader(new FileReader(file));
	        String line = in.readLine();
	        int n = Integer.parseInt(line);
	        
	        for (int i = 0; i < n; i++) {
	        	line = in.readLine();
	        	String[] words = line.split(" ");
	        	File img = new File(words[0]);
	        	int[] cl = new int[words.length - 1];
	        	for (int j = 1; j < words.length; j++) {
	        		cl[j - 1] = Integer.parseInt(words[j]);
	        	} 
	        	this.cFeatures.put(img, cl);
	        }	        
	        in.close();
	    } catch (IOException ioe) {
	    	if (in != null) {
	    		try { in.close(); } catch (Exception e) {};
	    	}
	    	return false;
	    }
	    return true;
	}
	
	private short[][] getNeighbors(File img) {
		IFeature[] features = ln.getFeatures(img);
		IFeature distances = features[0];
		float[][] dists = distances.getValues();

		
		short[][] neighs = new short[dists.length][elements - firstN];
		for (int i = 0; i < dists.length; i++) {
			for (int j = 0; j < elements - firstN; j++) {
				neighs[i][j] = (short)dists[i][j + firstN];
			}
		}
		return neighs;
	}
	
	private float[][] getParams(File img) {
		IFeature[] fs = ln.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getParams();
		return feat;
	}
	/*
	 * zwraca tablicę z numerami klastrów dla każdego punktu w obrazie
	 */
	private int[] getFeaturs(File img) {
		IFeature[] fs = lf.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getValues(); //feat[numer linii] = [tablica z cechami]
		int[] cFeat = new int[feat.length]; //tablica o takiej długości jak ilość linii
		for (int i = 0; i < feat.length; i++) {
			//do każdej linii (do każdego punktu) zwraca numer klastra
			cFeat[i] = this.cluster.cluster(feat[i]);
			//System.out.println("Assign: " + cFeat[i]);
		}
		 
		return cFeat;
	}
	
	public void init(File[] files) {
		System.out.println("Looking for images...");
		this.content = new ArrayList<File>();
		for (File file : files) {
			if (file!=null && file.getName().toLowerCase().endsWith(".sift")) {
				this.content.add(file);
			}
			else
				System.out.println(file);
		}
		File clusterFile = new File(Paths.path+this.clNum+".centroids");
		if (!clusterFile.exists()) {
			//System.out.print("Creating clustering data");
			List<float[]> cData = new ArrayList<float[]>();
			for (File img : this.content) {
				IFeature[] fs = lf.getFeatures(img);
				IFeature f = fs[0];
				float[][] feat = f.getValues();
				for (int i = 0; i < feat.length; i++) {
						cData.add(feat[i]);
				}
			}
			//System.out.println("Clustering data ("+cData.size()+")...");
			this.cluster = new FloatKMeans(clNum, 10);
			this.cluster.cluster(cData);
			cData = null;
			this.cluster.store(clusterFile);
		} else {
			System.out.println("Reading cluster centers...");
			this.cluster = new FloatKMeans(clNum, 10);
			this.cluster.read(clusterFile);
		}
		File featFile;
		if(filtering)
			featFile = new File(Paths.path+db+"-"+this.clNum+".feats");
		else 
			featFile = new File(Paths.path+db+"-"+this.clNum+"-temp.feats");
		// TODO zakomentowany if - zawsze tworzy nowy plik z przypisaniem do klastrów - potrzebne przy filtrowaniu siftów
		//if (!featFile.exists()) {
			//System.out.print("Assigning of cluster centers "+content.size());
			for (File img : this.content) {
				int[] cFeat = this.getFeaturs(img);
				//System.out.println(img+ " "+Arrays.toString(cFeat));
				this.cFeatures.put(img, cFeat);
				//float[][] cParams = this.getParams(img);
				//this.cParams.put(img, cParams);
			}
			this.storeFeatures(featFile);
		//} else {
		//	System.out.println("Read clustered features...");
		//	this.readFeatures(featFile);
		//}
		
		//System.out.print("Reading the neighborhood");
		for (File img : this.content) {
			short[][] neighs = this.getNeighbors(img);
			this.cNeighs.put(img, neighs);
		}
	}

	public float sim(int[] qf, short[][] qn, int[] df, short[][] dn, float[][] pq, float[][] pd, File query, File data) {
		float similarity = 0;
		// wersja filtrowana jest mocno przyspieszona
		if (filtering) {
			similarity = simFiltering(qf, qn, df, dn, pq, pd, query, data);
		}
		else
		{
			similarity = simNotFiltering(qf, qn, df, dn, pq, pd, query, data);

		}
		return similarity;
	}
	private int simFiltering(int[] qf, short[][] qn, int[] df, short[][] dn, float[][] pq, float[][] pd, File query, File data)
	{
		//szybkie
		int similarity=0;
		int[] qnnc; //bow sasiedztwa
		/*
		 * oryginalnie jest sprawdzane sąsiedztwo dla najlepszych punktow z danego klastra, ale tu mamy co najwyżej jeden 
		 * punkt dla każdego klastra więc lepiej jest iterować po punktach
		 * 
		 *  1. dla każdego punktu z query tworzymy bow sąsiedztwa 
		 *  2. similarity = ile punktów w tym sąsiedztwie jest przypasowane do tego punktu o tym samym klastrze w data 
		 */
		int[] claq = new int[clNum];  //klaster -> numer punktu (query)
		int[] clad = new int[clNum];  //klaster -> numer punktu (data)
		for(int i=0;i<qf.length;i++)
		{
			claq[qf[i]]=i;
		}
		for(int i=0;i<df.length;i++)
		{
			clad[df[i]]=i;
		}
		//qcla i dcla nie są potrzebne -> identyczne z qf i df
		int cnt;
		for(int i=0;i<qf.length;i++)
		{
			//po wszystkich punktach z query
			qnnc = new int[clNum];
			for(int j=0; j<qn[i].length; j++)
			{
				//po wszystkich sasiadach - tworzenie bowa sasiedztwa
				int qcl = qf[qn[i][j]]; //klaster sasiada
				qnnc[qcl]=1;
			}
			cnt=0;	
			int nd=clad[qf[i]]; //numer punktu w data z tego samego klastra
			for(int j=0; j<dn[nd].length;j++)
			{
				//po siasiadach tego punktu w data

				/*
				 * dn[nd][j] - numer punktu z sasiedztwa
				 * df[dn[nd][j]] - klaster tego punktu
				 * qnnc[df[dn[nd][j]]] - czy ten punkt pojawił się w query
				 */
				if(qnnc[df[dn[nd][j]]]==1)
				{
					cnt++;
					if(cnt>3)
					{
						similarity++; 
						break;
					}
				}
			}
			for(int j=0; j<qn[i].length; j++)
			{
				//po wszystkich sasiadach - zerowanie bowa sasiedztwa
				int qcl = qf[qn[i][j]]; //klaster sasiada
				qnnc[qcl]=0;
			}
		}
		return similarity;
	}
	
	private int simNotFiltering(int[] qf, short[][] qn, int[] df, short[][] dn, float[][] pq, float[][] pd, File query, File data)
	{
		int similarity=0;
		int maxPoints = 10;
		//cl - tablica zawierająca pierwsze 10 punktów zawartych w danym klastrze
		int[][] clq = new int[clNum][maxPoints];
		int[][] cld = new int[clNum][maxPoints];
		//il - bow
		int[] ilq = new int[clNum];
		int[] ild = new int[clNum];
		//tworzenie tablic dla obrazu-zapytania (query) i obrazu z którym go porównujemy (data) 
		for (int i = 0; i < qf.length; i++) {
			if (ilq[qf[i]] < maxPoints) {
				int clust = qf[i];
				int index = ilq[clust];
				clq[clust][index] = i;
				ilq[clust]++;
			}
		}
		for (int i = 0; i < df.length; i++) {
			if (ild[df[i]] < maxPoints) {
				cld[df[i]][ild[df[i]]] = i;
				ild[df[i]]++;
			}
		}
		//wielkosc najwiekszego sasiedztwa w klastrze
		int[] clam = new int[clNum];
		//numer punktu w zapytaniu dla ktorego sasiedztwo jest najwieksze (klaster -> punkt)
		int[] claq = new int[clNum];
		//numer punktu w bazie dla ktorego sasiedztwo jest najwieksze (klaster -> punkt)
		int[] clad = new int[clNum];
		//to samo co wyzej, tylko wskazniki w druga strone (z punktu do klastra)
		int[] qcla = new int[qf.length];
		int[] dcla = new int[df.length];
		for (int i = 0; i < qcla.length; i++) {
			qcla[i] = -1;
		}
		for (int i = 0; i < dcla.length; i++) {
			dcla[i] = -1;
		}
		int[] qnnc = new int[clNum];
		for (int i = 0; i < clNum; i++) {
			//dla każdego klastra
			for (int j = 0; j < ilq[i]; j++) {
				//dla kazdego punktu z danego klastra z zapytania
				int qp = clq[i][j]; //index tego punktu
				for (int nq = 0; nq < qn[qp].length; nq++) {
					//dla wszystkich sasiadów tego punktu z zapytania
					int qcl = qf[qn[qp][nq]];
					qnnc[qcl]++; //tworze BOW sasiedztwa dla punktu w zapytaniu 
				}
				for (int k = 0; k < ild[i]; k++) {
					//dla każdego punktu z danego klastra z data
					int dp = cld[i][k];
					int cnt = 0; //liczba pasujacych sasiadow w rozwazanej parze
					for (int nd = 0; nd < dn[dp].length; nd++) {
						//dla wszystkich sasiadów danego punktu w data
						int dcl = df[dn[dp][nd]]; //klaster tego punktu
						cnt += qnnc[dcl]; //zliczam pasujacych sasiadow
						//otrzymujemy przeciecie dwoch histogramow
					}

					if (cnt > clam[i]) {
						clam[i] = cnt; //najlepszy znaleziony (najwieksze sasiedztwo)
						claq[i] = qp; // do klastra przypisuje punkt
						clad[i] = dp; // do klastra przypisuje punkt
						qcla[qp] = i; // przypisuje klaster
						dcla[dp] = i; // przypisuje klaster
					}
				}
				for (int nq = 0; nq < qn[qp].length; nq++) {
					qnnc[qf[qn[qp][nq]]]--; //zeruje BOW sasiedztwa
				}
			}
		}
		for (int i = 0; i < clNum; i++) {
			//dla każdego klastra
			if (clam[i] > 0) { //czy jest niezerowe sasiedztwo dla danego klastra (wybranej pary)
				int cnt = 0;
				int qp = claq[i]; //punkt w zapytaniu
				int dp = clad[i]; //punkt w bazie

				for (int nq = 0; nq < qn[qp].length; nq++) {
					//po sąsiadach punktu w zapytaniu
					int qnp = qn[qp][nq]; //index sasiada
					int qnc = qcla[qnp];  //klaster tego punktu
					if ((qnc >= 0) && (claq[qnc] == qnp)) {
						//sprawdzenie czy w sasiedztwie tego punktu znajdują się inne punkty z dobrym sąsiedztwem  
						qnnc[qnc]++; //wypelniam BOW sasiedztwa (ale juz po filtrze)
					}
				}
				for (int nd = 0; nd < dn[dp].length; nd++) {
					//po sasiadach w data
					int dnp = dn[dp][nd]; 
					int dnc = dcla[dnp];
					if ((dnc >= 0) && (clad[dnc] == dnp)) {
						// tak samo - tylko dla punktów z dobrym sąsiedztwem 
						cnt += qnnc[dnc]; //sprawdzam przeciecie histogramow sasiedztwa
						
						if (!cntpp && cnt > 3) { //czy jest minimum 4 pasujacych sasiadow
							break;
						}
					}
				}
				for (int nq = 0; nq < qn[qp].length; nq++) {
					int qnp = qn[qp][nq];
					int qnc = qcla[qnp];
					if ((qnc >= 0) && (claq[qnc] == qnp)) {
						qnnc[qnc]--; //kasuje BOW sasiedztwa
					}
				}

				if (cnt > 3) {
					similarity++;
					pairs.add(new int[] {qp, dp});
				}
			}
		}
		return similarity;
	}
	
	public List<Ordered<File>> seek(File query) {
		int[] qf = this.cFeatures.get(query);
		short[][] qn = this.cNeighs.get(query);
		float[][] pq = this.cParams.get(query);
		long start = System.currentTimeMillis();
		List<Ordered<File>> res = new ArrayList<Ordered<File>>();
		for (File img : this.content) {
			if (img.getName().equals(query.getName())) {
				continue;
			}
			int[] df = this.cFeatures.get(img);
			short[][] dn = this.cNeighs.get(img);
			float[][] pd = this.cParams.get(img);
			
			float s = this.sim(qf, qn, df, dn, pq, pd, query, img);
			if (s > 0) {
				res.add(new Ordered<File>(img, s));
			}
		}
		long end = System.currentTimeMillis();
		time = end - start;
		Collections.sort(res);
		return res;
	}
	
	public void setContent(File[] con)
	{
		this.content = new ArrayList<File>();
		for (File file : con) {
			if (file!=null && file.getName().toLowerCase().endsWith(".png")) {
				this.content.add(file);
			}
			else
				System.out.println(file);
		}
	}
	public static void mainFiltrowanie(String db, String method, double part, int clNum, int firstN, int element, boolean cntpp) throws IOException
	{
		/*
		 * FILTROWANIE
		*/
		ClusteredSimilarityMass seek_filtered;
		ClusteredSimilarityMass seek_full;
		seek_filtered= new ClusteredSimilarityMass(db, method);
		seek_full= new ClusteredSimilarityMass(db, "");
		seek_full.filtering=false;
		seek_full.clNum = clNum;
		seek_full.firstN = firstN;
		seek_full.elements = element;
		seek_full.cntpp = cntpp;
		if(method.equals("minArea1000") || method.equals("maxArea1000")) seek_filtered.filtering=false;
		seek_filtered.clNum = clNum;
		seek_filtered.firstN = firstN;
		seek_filtered.elements = element;
		seek_filtered.cntpp = cntpp;
		String name_exper = "cms-mix_"+method+"_"+part+"_"+seek_full.clNum+"-"+(seek_full.unique ? "unique-" : "")+(seek_full.ellipse ? "ellipse-" : "")+seek_full.firstN+"-"+seek_full.elements;
		if(!new File(Paths.resPath+"mp-"+name_exper+"-matching").exists())
		{
			File dir =new File(Paths.imPath); 
			seek_filtered.init(dir.listFiles());
			seek_full.init(dir.listFiles());
			String[] imgs = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name)
				{
					return name.toLowerCase().endsWith(".png");
				}
			});
			Results res = new Results(db);
			List<Ordered<File>> result;
			//dla każdego obrazu wyszukiwanie obrazów podobnych:
			for(int i=0; i<imgs.length; i++)
			{
				result = seek_filtered.seek(new File(Paths.imPath+imgs[i]));
				//tu jest zakończony pierwszy etap - mamy wstępnie posortowane elementy - można teraz te wyniki odciąć w 
				//w pewnym miejscu i zrobić to lepsze przeszukiwanie
				File[] r_temp=new File[(int)(result.size()*part)];
				for(int j=0; j<(int)(result.size()*part); j++)
				{
					r_temp[j]=result.get(j).get();
				}
				seek_full.setContent(r_temp);
				
				result = seek_full.seek(new File(Paths.imPath+imgs[i]));
				res.add(imgs[i], result, seek_filtered.time+seek_full.time);		
			}
			
	
			res.generateHtml(name_exper);
			res.generateResults(name_exper);
			Chart ch = new Chart(seek_filtered.db);
			/*
			* koniec FILTROWANIE
			*/
		}
	}
	public static void mainBezFiltrowania(String db, int clNum, int firstN, int elements, boolean cntpp, boolean ellipse, boolean unique, String method) throws IOException {
		/*
		 * BEZ FILTROWANIA
		 */
		ClusteredSimilarityMass seek_full;
		seek_full= new ClusteredSimilarityMass(db, method);
		seek_full.filtering=false;
		seek_full.clNum = clNum;
		seek_full.firstN = firstN;
		seek_full.elements = elements;
		seek_full.cntpp = cntpp;
		seek_full.ellipse =ellipse;
		seek_full.unique = unique;
		String name_exper="cms-"+seek_full.name+"-"+seek_full.clNum+"-"+(seek_full.unique ? "unique" : "")+"-"+(seek_full.ellipse ? "ellipse" : "")+"-"+(seek_full.cntpp ? "cntpp" : "")+"-"+seek_full.firstN+"-"+seek_full.elements;
		if(!new File(Paths.resPath+"mp-"+name_exper+"-matching").exists())
		{
			File dir =new File(Paths.imPath); 
			seek_full.init(dir.listFiles());
			String[] imgs = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name)
				{
					return name.toLowerCase().endsWith(".png");
				}
			});
			Results res = new Results(db);
			List<Ordered<File>> result;
			//dla każdego obrazu wyszukiwanie obrazów podobnych:
			for(int i=0; i<imgs.length; i++)
			{
				result = seek_full.seek(new File(Paths.imPath+imgs[i]));
				res.add(imgs[i], result, seek_full.time);	
			}
			
			res.generateHtml(name_exper);
			res.generateResults(name_exper);
			Chart ch = new Chart(seek_full.db);
		/*
		 * koniec BEZ FILTROWANIA
		 */
		}
	}
	public static void main(String[] args) throws IOException {

//PARAMETRY
//		int clNum = 1000;
//		boolean unique = false;
//		boolean ellipse = false;
//		int elements = 40;
//		int firstN = 5;
//		boolean cntpp = false;
		int[] clNums = new int[] {500, 1000, 1500, 3000};
		int[] firstNs = new int[] {0, 5};
		int[] elements = new int[] {35,40,60};
		boolean[] cntpps = new boolean[] {true, false};
		double[] parts = new double[] {0.1, 0.3, 0.5};
		String[] filters = new String[] {"maxAreaCluster", "minAreaCluster", "minArea1000", "maxArea1000"};
		for(int element : elements)
			for(boolean cntpp : cntpps)
				for(int firstN : firstNs)
					for(int clNum : clNums)
					{
						System.out.println(element+" "+cntpp+" "+firstN+" "+clNum);
						for(String filter : filters)
							mainBezFiltrowania("matching", clNum, firstN, element, cntpp, false, false, filter);
							//for(double part : parts)
							//{
								//mainFiltrowanie("matching", filter, part, clNum, firstN, element, cntpp);
							//}
						mainBezFiltrowania("matching", clNum, firstN, element, cntpp, false, false, "");
						mainBezFiltrowania("matching", clNum, firstN, element, cntpp, false, true, "");
						mainBezFiltrowania("matching", clNum, firstN, element, cntpp, true, false, "");
					}
		
			
//		mainBezFiltrowania(args);
		
	}
}
