package basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

import javax.imageio.ImageIO;

import run.Paths;

public class Chart {
	String db;
	int[] colors=new int[] { 
							0xc44448,
							0x30845c,
							0xf0e848,
							0x343074,
							0xbc306c,
							0x2874c4,
							0xfce94f,
							0x8ae234,
							0xfcaf3e,
							0x729fcf,
							0xad7fa8,
							0xe9b96e,
							0xef2929,
							0xc4a000,
							0x4e9a06,
							0xce5c00,
							0x204a87,
							0x5c3566,
							0x8f5902,
							0xa40000,
							0xff00ff,
							0x6600ff,
							0x66cc00,
							0xffcc00
							}; 
	public Chart(String db) throws IOException
	{
		this.db=db;
		File dir = new File(Paths.resPath);
		File[] files0 = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return !name.toLowerCase().endsWith(".png") && !name.equals("mp-tdidf") && !name.equals("mp-random");
			}
		});
		Arrays.sort(files0, new Comparator()
	    {
	      public int compare(final Object o1, final Object o2) {
	        return new Long(((File)o1).lastModified()).compareTo
	             (new Long(((File) o2).lastModified()));
	      }
	    }); 
		File[] files = new File[files0.length+2];
		files[0]=new File(Paths.resPath+"mp-tdidf");
		files[1]=new File(Paths.resPath+"mp-random");
		for(int i=0;i<files0.length;i++)
			files[2+i]=files0[i];
		String[] names = new String[files.length];
		double[] times = new double[files.length];
		double[] precisions = new double[files.length];
		double[] recalls = new double[files.length];
		int time_max=0;

		
		
		
		for(int i=0; i<files.length;i++)
		{
			Scanner sc = new Scanner(files[i]);
			sc.next();
			names[i]=sc.next();
			String s = sc.next();
			if(s.equals("time:"))
				s=sc.next();
			times[i]=Integer.parseInt(s);
			if(times[i]>time_max) time_max=(int)times[i];
			precisions[i]=Double.parseDouble(sc.next());
			recalls[i]=Double.parseDouble(sc.next());
			//System.out.println(names[i]+" "+times[i]+" "+precisions[i]+" "+recalls[i]);

			//System.out.println(precisions[i]+" "+(int)(10+630-precisions[i]*630)+" "+(int)(precisions[i]*630));
		}
		
		
		//RYSOWANIE
		
		BufferedImage bi_precyzja = new BufferedImage(1100, 900, BufferedImage.TYPE_INT_RGB);
		BufferedImage bi_kompletnosc = new BufferedImage(1100, 900, BufferedImage.TYPE_INT_RGB);
		BufferedImage bi_czas = new BufferedImage(1100, 900, BufferedImage.TYPE_INT_RGB);
		
		Graphics g_precyzja = bi_precyzja.getGraphics();
		Graphics g_kompletnosc = bi_kompletnosc.getGraphics();
		Graphics g_czas = bi_czas.getGraphics();
		
		String[] ns = new String[] {"precyzja","kompletnosc","czas"};
		Graphics[] gs = new Graphics[] {g_precyzja, g_kompletnosc, g_czas};
		BufferedImage[] bis = new BufferedImage[] {bi_precyzja, bi_kompletnosc, bi_czas};
		double[][] vs = new double[][] {precisions, recalls, times};
		int[] maxs = new int[] {1, 1, time_max+500};
		
		for(int img=0; img<3; img++)
		{
			gs[img].fillRect(0, 0, 1100, 900); 
			gs[img].setColor(new Color(0x0));
			gs[img].setFont(new Font("Ubuntu", 0, 20));
			gs[img].drawString(""+ns[img], 400, 20);
			gs[img].setFont(new Font("Ubuntu", 0, 10));
			gs[img].drawLine(10, 0, 10, 650);
			gs[img].drawLine(0, 30, 750, 30);
			gs[img].drawLine(0, 620, 750, 620);
			
			for(int j=1;j<10;j+=1)
			{
				gs[img].setColor(new Color(0xaaaaaa));
				gs[img].drawLine(10, 30+j*59, 750, 30+j*59);
				gs[img].setColor(new Color(0x0));
				gs[img].drawString(Math.round((maxs[img]-j*maxs[img]/10.0)*10)/10.0+"", 720, 30+j*59);
			}
			gs[img].setColor(new Color(0x0));
			gs[img].drawString(maxs[img]+"", 2, 15);
			
			for(int i=0; i<files.length;i++)
			{
				gs[img].setColor(new Color(0x0));
				gs[img].drawString(names[i]+" ("+((int)(vs[img][i]*1000)/1000.0)+")", 800, 50+15*i);
				gs[img].drawString(i+"", 772, 48+15*i);
				gs[img].drawString(i+"", 11+15*i, 23+(int)(590-vs[img][i]/maxs[img]*590));
				gs[img].setColor(new Color(colors[i%colors.length]));
				gs[img].fillRect(787, 40+15*i, 10, 10);
				gs[img].fillRect(11+15*i, 30+(int)(590-vs[img][i]/maxs[img]*590), 14, (int)(vs[img][i]/maxs[img]*590));
			}
			gs[img].setColor(new Color(colors[0]));
			gs[img].drawLine(10, 30+(int)(590-vs[img][0]/maxs[img]*590), 750, 30+(int)(590-vs[img][0]/maxs[img]*590));
			gs[img].setColor(new Color(colors[2]));
			gs[img].drawLine(10, 30+(int)(590-vs[img][2]/maxs[img]*590), 750, 30+(int)(590-vs[img][2]/maxs[img]*590));
			ImageIO.write(bis[img], "png", new File(Paths.resPath+ns[img]+".png"));
		}
		
	}
	public static void main(String[] args) throws IOException
	{
		new Chart(args[0]);
	}
}
