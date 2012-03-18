/**
 * 
 */
package basic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.imageio.ImageIO;

import run.Paths;

/**
 * @author Agnieszka Glabala
 *
 * Tworzy obrazy z zaznaczonymi punktami przed i po filtrowaniu 
 */
public class DrawImage {
	String rPath = "/points/";
	String[] imgs;
	public DrawImage()
	{
		File dir = new File(Paths.imPath);
		imgs = dir.list();
	}
	/**
	 * Tworzy obraz składający się z 6 obrazów:
	 * 		|bez punktów    | przed filtrowaniem  |
	 *      |maxAreaCluster | minAreaCluster      |
	 *      |maxArea1000    | minArea1000         |
	 * @param img
	 * @return
	 * @throws IOException
	 */
	BufferedImage getImg(String img) throws IOException
	{
		System.out.println(Paths.imPath+img);
		BufferedImage im = ImageIO.read(new File(Paths.imPath+img));
		//Image im2 = im.getScaledInstance(im.getWidth(), im.getHeight(), Image.SCALE_SMOOTH);
		BufferedImage res = new BufferedImage(im.getWidth(), (im.getHeight()*3)/2, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = res.createGraphics();
		int w = im.getWidth()/2;
		int h =  im.getHeight()/2;
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), 0, 0);
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), w, 0);
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), 0, h);
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), w, h);
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), 0, h*2);
		g.drawImage(im, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5),AffineTransformOp.TYPE_BICUBIC), w, h*2);
		g.setColor(new Color(0xff0000));
		//BEZ FILTRA
		ArrayList<int[]> points = getPoints(new File(Paths.fPath+img+".haraff.sift"));
		g.drawString("all points", w+10, 10);
		for(int[] p : points)
		{
			g.fillOval(p[0]/2+w, p[1]/2, 2, 2);
		}
		//MaxAreaCluster
		points = getPoints(new File(Paths.fPath+img+".haraff.sift.maxAreaCluster"));
		g.drawString("maxAreaCluster", 10, h+10);
		for(int[] p : points)
		{
			g.fillOval(p[0]/2, p[1]/2+h, 2, 2);
		}
		//MinAreaCluster
		points = getPoints(new File(Paths.fPath+img+".haraff.sift.minAreaCluster"));
		g.drawString("minAreaCluster", w+10,h+10);
		for(int[] p : points)
		{
			g.fillOval(p[0]/2+w, p[1]/2+h, 2, 2);
		}
		//MaxArea1000
		points = getPoints(new File(Paths.fPath+img+".haraff.sift.maxArea1000"));
		g.drawString("maxArea1000", 10, h*2+10);
		for(int[] p : points)
		{
			g.fillOval(p[0]/2, p[1]/2+h*2, 2, 2);
		}
		//MinArea1000
		points = getPoints(new File(Paths.fPath+img+".haraff.sift.minArea1000"));
		g.drawString("minArea1000", w+10, h*2+10);
		for(int[] p : points)
		{
			g.fillOval(p[0]/2+w, p[1]/2+h*2, 2, 2);
		}
		return res;
	}
	private ArrayList<int[]> getPoints(File sift) throws FileNotFoundException
	{
		Scanner sc = new Scanner(sift);
		sc.nextLine();sc.nextLine();
		int n=0;
		ArrayList<int[]> arr = new ArrayList<int[]>();
		while(sc.hasNextLine())
		{
			int[] p = new int[2];
			p[0]=(int)Double.parseDouble(sc.next());
			p[1]=(int)Double.parseDouble(sc.next());
			arr.add(p);
			sc.nextLine();
			n++;
		}
		System.out.println("\t"+sift+": "+n);
		return arr;
	}
	public static void main(String[] args) throws IOException
	{
		DrawImage di = new DrawImage();
		for(String img : di.imgs)
		{
			ImageIO.write(di.getImg(img), "png", new File(Paths.path+di.rPath+img));
		}
	}
}
