package basic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;


public class BOW {
	int clNum;
	int[] nOfDocs;
	int D;
	File[] files;
	private int[][] bows;
	public int[][] clusters; //file -> point -> cluster
	double tf[][];
	//static String path = "/home/agnis/Desktop/AnalizaObrazow/";
	//static String path = "/files/workspace/AnalizaObrazow/";
	static String path = "/home/ljercinski/AnalizaObrazow/oxbuildings/";
	//static String path = "/media/AD81-FCC4/AnalizaObrazow/";
	static String BOWPath = path+"bows/";
	static String clusterPath;
	static String matrixPath = path+"matrices/";
	static String imagesPath = path+"images/";
	public BOW(File[] files, int clNum) {
		this.files = files;
		D = files.length; //liczba wszystkich dokumentów
		nOfDocs = new int[clNum]; //liczba dokumentów zawierająca term i
		this.clNum = clNum;
		clusterPath = path+"clusters"+clNum+"/";
	}
	
	void getBow(int i) {
		int[] bow = new int[clNum];
		int[] arr;
		Scanner sc;
		try {
			sc = new Scanner(new File(clusterPath+files[i].getName()+".haraff.sift.cl"+clNum));		
			sc.nextLine();//ile wartości
			arr = new int[Integer.parseInt(sc.nextLine())]; //ile punktów
			int j=0;
			String[] tmp;
			int tmp2;
			while(sc.hasNextLine()) {
				tmp = sc.nextLine().split(" ");
				//tmp2 = numer klastra dla jtej linii
				tmp2=(int)Float.parseFloat(tmp[tmp.length-1]);
				arr[j]=tmp2;
				j++;
				bow[tmp2]++;
				//jeśli klaster pojawił się pierwszy raz w dokumencie, dodaj do licznika dokumentów dla tego klastra
				if(bow[tmp2]==1) {
					nOfDocs[tmp2]++;
				}
			}
			bows[i] =  bow;
			clusters[i] = arr;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	/**
	 * liczy bowa i czyta z pliku z przypisaniami do klastra
	 * uzupełnia bow i clusters
	 */
	public void allBows() {
		bows = new int[files.length][];
		clusters=new int[files.length][];
		for(int i=0; i<files.length; i++) {
				getBow(i);
		}
	}
	
	double idf(int i) {
		return Math.log(D/(1.0+nOfDocs[i]));
	}
	
	public void evaluateTF() {		
		tf = new double[files.length][clNum];
		for(int i=0;i<files.length;i++) {
			//dla każdego dokumetu (bowa)
			int nOfTerms=0;
			for(int j=0; j<clNum; j++) {
				//dla każdego termu
					nOfTerms+=getBows()[i][j];
			}
			for(int j=0; j<clNum; j++) {
				tf[i][j]=getBows()[i][j]*1.0/nOfTerms;
			}
		}
	}
	
	void writeBOWS(String outF, int[][] bows) throws IOException {
		  BufferedWriter out = new BufferedWriter(new FileWriter(outF));
		  out.write(clNum+"\n");
		  out.write(D+"");
		  
		  for(int i=0; i<bows.length; i++) {
			  out.write("\n"+files[i].getName());
			  for(int j=0; j<bows[i].length; j++) {
				  out.write(" "+(int)bows[i][j]);
			  } 
		  }
		  out.close();
	}
	/**
	 * i, j wektory bow z bazy
	 * @param i
	 * @param j
	 * @return
	 */
	public double distTFIDF(int i, int j) {
		double d = 0;
		for (int k = 0; k < clNum; k++) {
			d+=tf[i][k]*tf[j][k]*(bows[i][k]-bows[j][k])*(bows[i][k]-bows[j][k]);
		} 
		
		return Math.sqrt(idf(i)*idf(j)*d);
	}
	
	public double distTFIDFl1(int i, int j) {
		double d = 0;
		for (int k = 0; k < clNum; k++) {
			d+=tf[i][k]*tf[j][k]*Math.abs(bows[i][k]-bows[j][k]);
		} 
		return idf(i)*idf(j)*d;
	}
	
	public double dist(int i, int j) {
		double d = 0;
		for (int k = 0; k < clNum; k++) {
			d+=(bows[i][k]-bows[j][k])*(getBows()[i][k]-getBows()[j][k]);
		} 
		return Math.sqrt(d);
	}
	
	double[][] evaluateMatrix() {
		double[][] matrix = new double[files.length][files.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				matrix[i][j]=dist(i,j);
			}
		}
		return matrix;
	}
	
	void writeMatrix(String outF, double[][] matrix) throws IOException {
		 BufferedWriter out = new BufferedWriter(new FileWriter(outF));
		  
		  for(int i=0; i<files.length; i++) {
			  out.write(";"+files[i].getName());
		  }
		  for(int i=0; i<files.length; i++) {
			  out.write("\n"+files[i].getName());
			  for(int j=0; j<matrix[i].length; j++) {
				  out.write(";"+matrix[i][j]);
			  } 
		  }
		  out.close();
	}
	
	public static void main(String[] args) throws IOException {
		BOW b = new BOW(new File(imagesPath).listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith("png")) return true;
				return false;
			}
		}), 15000);
		b.allBows();
		b.writeBOWS(BOWPath+"bow-"+b.clNum, b.getBows());
		b.evaluateTF();
		b.writeMatrix(matrixPath+"bow-"+b.clNum, b.evaluateMatrix());
	}

	public int[][] getBows() {
		return bows;
	}

	public void setBows(int[][] bows) {
		this.bows = bows;
	}
}
