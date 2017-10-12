#!/bin/sh

if [ -z $1 ]; then
   echo 'usage ./extract_profiling.sh <jobid>'
   exit
fi

sh5util -j $1 
sh5util -j $1 -E -l Node:Timeseries -s Energy -o extract_power_time_series.csv
sh5util -j $1 -E -l Node:Timeseries -s Tasks -o extract_util_time_series.csv
