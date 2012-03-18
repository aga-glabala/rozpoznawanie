#/usr/bin/env python
#-*- coding: utf-8 -*-
import os
import string
import sys
path = '/files/Projekty/Bazy/'+sys.argv[1]+'/'
interest_points = 'haraff'
#descriptors = 	  ['jla', \
#		   'sift', \
#		   'msift', \
#		   #-gloh', \
#		   #'-mom', \
		   #'-koen', \
		   #'-cf', \
		   #'-sc', \
#		   'spin', \
#		   'gpca', \
#		   'cc']
descriptors=['sift']

output = 'o1'
i = 0
l = len(os.listdir(path+"images"))
for f in os.listdir(path+"images"):
	for d in descriptors:
		print i, '/', l
		i+=1
		#if not os.path.exists(path+"features/"+interest_points+'-'+d):
		#	os.mkdir(path+"features/"+interest_points+'-'+d)
		#print '----------'+interest_points+d
		os.system("./extract_features_32bit.ln -"+interest_points+" -"+d+" -"+output+" "+path+"features/"+f+'.'+interest_points+"."+d+" -i "+path+"images/"+f)
