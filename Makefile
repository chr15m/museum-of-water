public/media/data-csv.txt: 180130\ Museum\ of\ Water\ iPad\ List\ .xls
	xls2csv -s cp1252 -d 8859-1 -x "$<" > $@

180130\ Museum\ of\ Water\ iPad\ List\ .xls:
	dropbox -sp download "MUSEUM OF WATER PHOTOS/180130 Museum of Water iPad List .xls"
