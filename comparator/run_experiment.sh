#!/bin/sh

if [ $# -ne 3 ]; then
  echo "The idea behind the usage of this script is that different implementations of the same application are to be comapared with the same workload."
  echo "This therefore generates a ranking for an application based upon its affinity towards each accelerator type."
  echo "Script usage: run_experiment.sh <script_to_submit> <script_name> <accelerator_info>"
  exit;
fi

#Run job, redirect output into file and parse out job id
sbatch $1 1> job.pid
sed -i 's/[^0-9]*//g' job.pid

echo "Starting to wait for job completion"
#Wait for it to finish

#ensure slurm has had time to queue the job
sleep 5
while true; do
  input=$( sacct -j $(cat job.pid) )
  #echo $input
  if [[ ! $input =~ .*COMPLETED.* ]]; then
    echo -n "#"
    sleep 10
  else
    break
  fi
done

echo " "
echo "completed waiting."
echo "extracting profile data"
#Extract profiling data
./extract_profiling.sh $(cat job.pid)

#Merge records together
./merge.sh 

#Get total energy consumption
sacct -j $(cat job.pid) --noconvert -n -o "consumedenergy" > energy.out
sed -i '/^\s*$/d' energy.out
sed -i 's/[^0-9]*//g' energy.out

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