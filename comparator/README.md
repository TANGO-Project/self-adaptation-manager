# Self Adaptation Manager - Implementation Comparison Scripts

&copy; University of Leeds 2017

The Tango Self-Adaptation Manager (SAM) is a component of the European Project TANGO (http://tango-project.eu ).

SAM is distributed under a [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Description

The aim of these scripts are to provide a means of comparing different implementations of the same application. 

The main files are:

* run_experiment.sh - This submits a job to slurm and records data such as power consumption and energy usage.
* post_run_processing.sh - This performs the post run analysis. In the event the script had already been run.
* getRanking.sh - This takes the gathered data and determines average completion time and energy consumption for each different accelerator based implementation.

Usage:

The 3 main scripts usage is as follows:

```
run_experiment.sh <script_to_submit> <script_name> <accelerator_info>
```

The first parameter <script_to_submit> indicates the name of the script to submit to sbatch.
The second parameter <script_name> indicates a human readable name for the script. 
The third <accelerator_info> indicates what accelerators were in use, such as gpu, cpu+gpu, gpu etc

```
post_run_processing.sh <job_id> <script_name> <accelerator_info>
```

This script is to be run in cases where the job has already been run previously and the data is to be analysed and added to the existing dataset.
It therefore works in a similar fashion to the command above. 

```
getRanking.sh <script_name>
```

This command filters the gathered data for a given scripts name, it then determines the average power consumption of each implementation of the named application.
