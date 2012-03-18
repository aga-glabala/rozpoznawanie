# -*- coding: utf-8 -*-
import os
import Image
import string

def jpg2png(db):
	#os.mkdir('images')#katalog docelowy
	for img in os.listdir(db+'imagesJPG'):#katalog źródłowy
		im=Image.open(db+'imagesJPG/'+img)
		name=string.split(img, '.')[0]
		im.save(db+'images/'+name+'.png')
	
	

def scale(db):
	for f1 in os.listdir(db+'/images_org/'):
		f, ext = os.path.splitext(f1)
		im = Image.open(db+'/images_org/'+f1)
		w,h=im.size
		if w>h:
			im.thumbnail((600,(600*h)/w), Image.ANTIALIAS)
		else:
			im.thumbnail(((w*800)/h,800), Image.ANTIALIAS)
		im.save(db+'/images/'+f + ".png", "PNG")
#scale('matching')
#scale('MGV2006')
jpg2png('/files/Projekty/Bazy/oxford/')
