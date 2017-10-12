#!/bin/sh

if [ $# -ne 1 ]; then
  echo "The idea behind the usage of this script is that different implementations of the same application are to be comapared with the same workload."
  echo "This therefore from existing ranking data for a named application indicates which accelerator implementation is fastest."
  echo "Script usage: post_run_processing.sh <script_name>"
  exit;
fi


awk -v name="$1" -F ',' '/,/{gsub(/ /, "", $0); if ($1 == name) print $0 }' Measurements.csv | cat > Filtered.csv
awk -F ',' -f average.awk Filtered.csv 
