#!/bin/sh

if [ $# -ne 3 ]; then
  echo "The idea behind the usage of this script is that different implementations of the same application are to be comapared with the same workload."
  echo "This therefore generates a ranking for an application based upon its affinity towards each accelerator type."
  echo "Script usage: post_run_processing.sh <job_id> <script_name> <accelerator_info>"
  exit;
fi

echo $1 | cat > job.pid

echo "extracting profile data"
#Extract profiling data
./extract_profiling.sh $(cat job.pid)

#Merge records together
./merge.sh 

#Get total energy consumption
sacct -j $(cat job.pid) --noconvert -n -o "consumedenergy" > energy.out
sed -i '/^\s*$/d' energy.out
sed -i 's/[^0-9]*//g' energy.out
if [ $(cat runtime.out | tr -cd ':' | wc -c) eq 2 ]; then
  cat runtime.out | awk -F: '{ print ($1 * 3600) + ($2 * 60) + $3 }' | cat >runtime.out

#get total runtime
sacct -j $(cat job.pid) --noconvert -n -o "elapsed" | head -1 > runtime.out
sed -i 's/^ *//;s/ *$//' runtime.out
if [[ $(cat runtime.out | tr -cd ':' | wc -c) == 2 ]]; then
   ans=$(cat runtime.out | awk -F: '{ print ($1 * 3600) + ($2 * 60) + $3 }')
   echo "$ans" > runtime.out
fi
if [[ $(cat runtime.out | tr -cd ':' | wc -c) == 3 ]]; then
   ans=$(cat runtime.out | awk -F: '{ print ($1 * 86400) ($2 * 3600) + ($3 * 60) + $4 }')
   echo "$ans" > runtime.out
fi


echo $2 "," $(awk -F ',' '{sum+=$1} END {print sum}' energy.out) "," $(cat runtime.out)  "," $(cat job.pid) "," $3 | cat >> Measurements.csv

#Rank Potential Implementations
sort -t, -nk2 Measurements.csv > Power_Ranking.csv
sort -t, -nk3 Measurements.csv > Time_Ranking.csv
