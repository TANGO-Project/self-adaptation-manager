#!/bin/bash

CORES=16

#take an input parameter of how many cores are available
if [ $# -eq 0 ]
then
CORES=16
else
CORES=$1
fi

#Add overall cpu utilisation column divide utilisation by core count
awk -v var=$CORES 'FS="," {print (NR==1)? $0",Util,Run,Progress" : $1","$2","$3","$4","$5","$6 ","$7","$8","$9","$10","$11","$12","$13","$8/var","int($4/120.5)","$4%120.5}' extract_util_time_series.csv  > util
#join files together 
join -t',' -1 4 -2 5 -o 1.1,1.2,1.3,1.4,1.5,2.1,2.2,2.3,2.4,2.5,2.6,2.7,2.8,2.9,2.10,2.11,2.12,2.13,2.14,2.15,2.16 extract_power_time_series.csv util > merged.csv
#join a limited set of values out for the calibration data
join -t',' -1 4 -2 5 -o 2.2,2.4,2.5,2.6,2.7,2.8,2.14,1.5,2.15,2.16 extract_power_time_series.csv util > selectedout
awk 'FS="&" {print $1","$2}' selectedout > calibration_data2.csv
#Add couter column and its header showing how consistant the power value is with its previous value
awk 'FS="," {print $0,(NR==1)?"Counter" : ($8==prev)?++count:count=1;prev=$8}' calibration_data2.csv > calibration_data.csv

#cleanup
rm util selectedout calibration_data2.csv
