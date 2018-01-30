public/media/data-csv.txt: public/media/180125\ Museum\ of\ Water\ Label\ \ Donors\ List.xls
	xls2csv -s cp1252 -d 8859-1 -x "$<" > $@
