package basic;
/**
 * @author Agnieszka Glabala
 *
 * Tworzy plik z wynikami (czas, precyzja, kompletność)
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.Paths;

public class Results {
	ArrayList<String> classes;
	String[] matching_classes = new String[] {"K3", "K2", "K1", "K5", "K4", "HD", 
					"R2", "R3", "Z4", "Z5", "Z6", "Z7", "Z1", "Z2", "Z3", "P2", "P3",
					"P1", "R1", "P4", "P5", "IP", "T2", "T1", "H2", "H1", "B1", "B2",
					"B3"};
	String [] mgv_classes = new String[] {"meadow", "people", "wall", "house", "yellow",
					"lake", "street", "zebra", "sea", "parking", "earth", "ship", 
					"sunset", "light", "cloud", "apple", "blue", "mountain", "flower",
					"winter", "floor", "purple", "rose", "window", "sky", "snow", 
					"outside", "fog", "speaker", "forest", "bus", "black", "white", 
					"kitchen", "beach", "nightsky", "red", "pink", "cluseup", "city", 
					"track", "poster", "night", "water", "podium", "plane", "closeup", 
					"waves", "sun", "outisde", "sign", "table", "desert", "man", 
					"building", "wood", "land", "tank", "brown", "car", "inside", 
					"grass", "tree", "strawberry", "sand", "fruit", "green", "pattern", 
					"gray", "rock", "builiding", "river", "runway", "road"};
	
	ArrayList<SubResult> res;
	long time;
	String db;
	public Results(String db)
	{
		this.db=db;
		res=new ArrayList<SubResult>();
		if(db.equals("matching")) classes=new ArrayList<String>(Arrays.asList(matching_classes));
		else classes=new ArrayList<String>(Arrays.asList(mgv_classes));
	}
	public void generateHtml(String name) throws IOException
	{
		BufferedWriter html = new BufferedWriter(new FileWriter(Paths.htmlPath+"/mp-"+name+"-"+db+".html"));
		html.write("<html><head><title>wyniki</title><style>"+ 
			   	"table {font-size:10px;font-family: 'Ubuntu';}"+
			   	".match {border: solid 5px green;}"+
			   	".image {display: inline}"+
			    "table.c tr{border: solid 5px black;}"+
				"</style></head><body>\n<table class=\"c\">\n");
		for(SubResult r : res)
		{
			html.write("<tr >\n<td>\n<table class=\"image\">\n<tr>\n<td>"+
					"<img class=\"match\" src=\"../images_mini/"+r.name+"\" />\n</td>"+
					"</tr>\n<tr><td class=\"caption\">"+r.name+"\n</td>\n</tr>\n</table>\n</td>"+
					"<td>\n");
			for (int i = 0; i < Math.min(10, r.subRes.size()); i++) {
				html.write("<table class=\"image\">\n<tr>\n<td>\n"+
						"<img src=\"../images_mini/"+r.subRes.get(i).get().getName()+"\" />"+
						"</td>\n</tr>\n<tr><td class=\"caption\">"+r.subRes.get(i).get().getName()+
						" (" + r.subRes.get(i).value() + ")</td>\n</tr>\n</table>\n");
			}
		}
		html.write("</table></body></html>");
		html.close();
	}
	
	public void generateResults(String name) throws IOException
	{
		BufferedWriter wyniki = new BufferedWriter(new FileWriter(Paths.resPath+"mp-"+name+"-"+db));
		
		double sp=0,sk=0;
		String temp=""; 
		for(String klasa : classes)
		{
			double c=0,r=0,e=0;
			for(SubResult sr : res) //wszystkie obrazy z przewidywaniami
			{
				if(sr.subRes.size()>0) {
					String[] dobra_anot=getAnot(sr.name);
					String[] przew_anot=getAnot(sr.subRes.get(0).get().getName());
					//System.out.println(klasa+" "+Arrays.toString(dobra_anot)+" "+Arrays.toString(przew_anot)+" "+in(dobra_anot, klasa)+" "+in(przew_anot, klasa)+" "+(in(przew_anot, klasa) && in(przew_anot, klasa)));
					
					if(in(przew_anot, klasa)) r++;
					if(in(dobra_anot, klasa)) e++;
					if(in(przew_anot, klasa) && in(dobra_anot, klasa)) c++;
				}
			}
			//System.out.println(true && false);
			//System.out.println(false && true);
//			//wyniki dla konkretnej klasy
//			c/=res.size();
//			e/=res.size();
//			r/=res.size();
			double tmp_p=0, tmp_k=0;
			if(r!=0) tmp_p=c/r;
			if(e!=0) tmp_k=c/e;
			temp+=(klasa+"\t"+tmp_p+"\t"+tmp_k+"\n");
			//temp+=klasa+" "+c+" "+r+" "+e+"\n";
			sp+=tmp_p;
			sk+=tmp_k;
		}
		sp/=classes.size();
		sk/=classes.size();
		wyniki.write("name: "+name+"\n"+time+"\t"+sp+"\t"+sk+"\n");
		wyniki.write(temp);
		wyniki.close();
	}
	
	public void add(String name, List<Ordered<File>> subRes, long time)
	{
		res.add(new SubResult(name, subRes, time));
		this.time+=time;
	}
	public boolean in(String[] tab, String klasa)
	{
		for(String t : tab)
		{
			if(t.equals(klasa)) return true;
		}
		return false;
	}
	private String[] getAnot(String name)
	{
		String[] anot;
		if (db.equals("matching"))
		{
			String[] s=name.split("\\.");
			//System.out.println(name);
			anot=s[0].split("_");
			anot=Arrays.copyOfRange(anot, 1, anot.length);

		}
		else if(db.equals("MGV2006"))
		{
			anot=name.split("_");
			anot=Arrays.copyOfRange(anot, 0, anot.length-1);
		}
		else
			anot= new String[] {"zła nazwa bazy"};
		return anot;
			
	}
	private class SubResult{
		List<Ordered<File>> subRes;
		long time;
		String name;
		public SubResult(String name, List<Ordered<File>> subRes, long time)
		{
			this.name = name;
			this.subRes = subRes;
			this.time = time;
		}
	}
	
}
