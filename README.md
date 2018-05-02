# Self Adaptation Manager

&copy; University of Leeds 2017

The Tango Self-Adaptation Manager (SAM) is a component of the European Project TANGO (http://tango-project.eu ).

SAM is distributed under a [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Description

The Self-Adaptation manager is responsible for co-ordinating the adaptive behaviour of the Tango architecture. The main aim of this adaptation is provide low power and energy usage while maintaining quality of service aspects of applications.

The primary mechanism by how this self-adaptation manager works is the generation of events, which in turn has actions associated with the events. 

## Installation Guide

This guide it is divided into two different guides, one specific to compilation of the Self Adaptation Manager and the second on how to run and configure the SAM.

### Compilation

#### Requirements

The SAM's primary prerequisites are:

* Java
* Maven
* Tango Energy Modeller

#### Installation and configuration procedure

To compile the self-adaptation manager, the following steps must be performed:
1.	Generate the self-adaptation manager jar using the command: mvn clean package (executed  in the self-adaptation manager directory)
2.	Install the energy modeller.
3.	Configure the SAM.  

#### Build status from Travis-CI

[![Build Status](https://travis-ci.org/TANGO-Project/self-adaptation-manager.svg?branch=master)](https://travis-ci.org/TANGO-Project/self-adaptation-manager)

#### Sonar Cloud reports:
The Sonar Cloud reports for this project are available at: https://sonarcloud.io/dashboard?id=eu.tango%3Aself-adaptation-manager

### Installation for running the service

In this case, we are going to detail how to run the application so that it can manage the adaptation in a Tango compliant environment.

#### Configuring the service

The SAM is highly configurable and can be configured using the following files in order to change its behaviour:

*self-adaptation-manager.properties* Holds properties such as which event assessor and decision engine to use.  
*self-adaptation-manager-threshold.properties* -	This holds settings regarding thresholds before a response occurs.  
*QoSEventCriteria.csv* - This specify the QoS rules that are required to create events. These events are later handled by the set of adaptation rules contained within the rules.csv file.  
*CronEvents.csv* - This specifies events based upon cron rules, this is an additional source of events, which can be launched by schedule.  
*rules.csv* -	This holds the listing of rules used for the StackedThresholdEventAssessor and ThresholdEventAssessor, which indicate how to perform the adaptation, based upon the events that are created.  

These settings must be tailored to the specific infrastructure. The settings are described below and an example of the settings is provided for reference.

#### self-adaptation-manager.properties

This file is the main configuration file for the SAM. An example is provided below:

```
self.adaptation.manager.environment.monitor.datasource=CollectdDataSourceAdaptor
self.adaptation.manager.history.length = 300
self.adaptation.manager.history.poll.interval = 5
self.adaptation.manager.event.assessor= StackedThresholdEventAssessor
self.adaptation.manager.decision.engine = RandomDecisionEngine
self.adaptation.manager.logging = true
```

The first parameter self.adaptation.manager.environment.monitor.datasource. Indicates what should be used as the data source for the self-adaptation manager, the default is: CollectdDataSourceAdaptor, but alternatively 
CollectDInfluxDbDataSourceAdaptor, SlurmDataSourceAdaptor or TangoEnvironmentDataSourceAdaptor may be used instead. It represents the source of data coming in for the environment monitor, whereby it will check that metrics arriving do not exceed the thresholds set.

This includes parameters such as how long to keep history records for in seconds, using the field:  self.adaptation.manager.history.length, as well as the rate at which this history log is cleared using the poll interval field: 

self.adaptation.manager.history.poll.interval. 

Monitoring events are assessed using an event assessor that is specified using the self.adaptation.manager.event.assessor field. The possible options for this field are: StackedThresholdEventAssessor, ThresholdEventAssessor. Once an event has been assessed to determine the type of action to take if any a decision engine is used to determine the scale and exact position of the adaptation i.e. which host or application. The decision engine used is determined by the field: self.adaptation.manager.decision.engine. This can be either: RandomDecisionEngine, LastTaskCreatedDecisionEngine or PowerRankedDecisionEngine.

The field self.adaptation.manager.logging indicates if the EventLog.csv and ResponseLog.csv files should be created. These logs record the arrival of events and the response of the SAM to the events.

#### self-adaptation-manager-threshold.properties

This file specifies the threshold of how many similar events must be seen before the SAM reacts. An example is provided below:

```
self.adaptation.manager.threshold = 0
```

#### QoSEventCriteria.csv - Used to specify the QoS rules that are required to create Events

This file specifies the basic thresholds that invoke QoS events that apply to all applications and to the infrastructure.

The file has the following parameters that should be added per each line:

* Unique Id - A number that identifies the rule
* Agreement Term - The value that is to be monitored, such as: 
* Comparator - Indicating values such as  LT, LTE, EQ, GT, and GTE
* Event Type - Indicating on the QoS rule firing if it generates an SLA_BREACH or a WARNING
* Guarantee Value - The threshold for the guarantee

The agreement term can be more complex than most. It can simply be a term that the monitoring infrastructure reports to the self-adaptation manager. The list of these recognised terms is printed out in the file when the application first runs: RecognisedTerms.csv.

It is possible to limit these QoS metrics to work against only a single host, following the format:

```
HOST:<hostname>:<metric>
```

such as: 

```
HOST:ns32:power
```

It can also be values such as: IDLE_HOST, IDLE_HOST+ACCELERATED, IDLE_HOST+PENDING_JOB, HOST_DRAIN, HOST_FAILURE, APP_FINISHED or CLOSE_TO_DEADLINE in the case of event generated by monitoring the infrastructure manager. 

The last format that can be used specifies application power consumption based events, in which the following pattern must be met. 

```
app_power:<APP_NAME>:<DEPLOYMENT_ID>
```

an example of this is:

```
app_power:RK-Bench-Test:*
```

An example of a complete QoS event criteria file is:

```
Unique Id,Agreement Term,Comparator,Event Type (SLA_BREACH or WARNING),Guarantee Value
1,IDLE_HOST,EQ,WARNING,0
2,APP_FINISHED,EQ,WARNING,0
3,CLOSE_TO_DEADLINE,EQ,WARNING,0
4,CPUTot,EQ,WARNING,0
5,power,GT,SLA_BREACH,400
6,cpu-measured,GT,WARNING,50
7,HOST:ns50:cpu-measured,GT,WARNING,1
8,app_power:COMPSs:*,GT,SLA_BREACH,300
9,app_power:RK-Bench-Test:*,GT,SLA_BREACH,50
10,APP_STARTED,EQ,WARNING,0
```


#### CronEvents.csv - This is used to specify a source of Events that are based upon the time. This allows for adaptation to occur to a schedule.

This table has three fields, the Unique Id, Agreement Term and the Cron Statement. An example of this file is shown below.

```
Unique Id,Agreement Term,Cron Statement
1,cron_test,0 0/1 * 1/1 * ? *
2,end_of_working_day,0 0 17 ? * MON-FRI *
```

This first cron statement fires an event that has the agreement term cron_test, with the cron schedule of triggering every minute. A more practical cron statement such as the second creates an event indicating the end of the working day. These events can then be utilised by a rule using the following parameters (see rules.csv):

```
Agreement Term: <as_specified>	
Direction: EQ
Event Type: WARNING
Lower bound: 0
Upper bound: 0
```

A cron statement can be written by using guidance from: http://www.cronmaker.com/. It is envisaged that cron rules could be used in cases such as unpausing applications that had been paused during the working day. This might be a stratergy used for example to limit power consumption during peak times. 

#### rules.csv – Used for the Threshold Event Assessor and Stacked Threshold Event Assessor

This file specifies the mapping between events and the adaptation that is to occur. These events having previously been specified in the QoSEventCriteria.csv and CronEvents.csv files. This file works in conjunction with the self-adaptation-manager-threshold.properties file, which sets the boundary condition for how many instances of an event must occur before action is taken. The rules file has three fields that must exist and are common to both StackedThresholdEventAssessor and the ThresholdEventAssessor. These are: the agreement term, the direction and the response type.

```
Agreement Term	      Direction	   Response Type
IDLE_HOST+ACCELERATED     EQ        UNPAUSE_APP
cpu-measured              GT        PAUSE_APP
HOST:ns50:cpu-measured    GT        PAUSE_APP
```

The first is the agreement term. This can be the name of any metric that is provided by the monitored infrastructure. In addition it can be based upon the following specialised events:

* IDLE_HOST - This creates an event in cases where a host becomes idle.
* IDLE_HOST+ACCELERATED - This creates an event in cases where a host becomes idle and in addition has an accelerator that could be used.
* IDLE_HOST+PENDING_JOB - This event indicates a host is idle and has a pending job waiting for other resources.
* HOST_DRAIN - This event indicates that a physical host has been marked to drain of jobs
* HOST_FAILURE - This event indicates that a physical host has failed
* APP_FINISHED - This event indicates that a job has finished
* CLOSE_TO_DEADLINE - This event indicates a job is close to its deadline, (where it will be ejected from the infrastructure).

These special agreement terms such as APP_FINISHED, in the event of a notification being created, generate Warnings with a comparator of EQ and guaranteed value and actual value of 0.

The next field is the direction, this indicates if the breach value is higher or lower than the metric value specified. The possible values are LT, LTE, EQ, GT, and GTE. i.e. less than, less than or equal, equals, greater than and greater than or equal to. 

The final field indicates the response type. The possible values are:  

```
INCREASE_WALL_TIME, REDUCE_WALL_TIME, MINIMIZE_WALL_TIME_SIMILAR_APPS, 
PAUSE_APP, UNPAUSE_APP, PAUSE_SIMILAR_APPS, UNPAUSE_SIMILAR_APPS,
OVERSUBSCRIBE_APP, EXCLUSIVE_APP,
RESELECT_ACCELERATORS,
KILL_APP, HARD_KILL_APP, KILL_SIMILAR_APPS
SHUTDOWN_HOST, STARTUP_HOST
REDUCE_POWER_CAP, INCREASE_POWER_CAP, SET_POWER_CAP
```

There are additional response types planned of:

```
ADD_TASK, REMOVE_TASK, SCALE_TO_N_TASKS, 
ADD_CPU, REMOVE_CPU, 
ADD_MEMORY, REMOVE_MEMORY
```

The Stacked Threshold Event assessor has additional optional arguments, which allow for more complex behaviour.

```
Agreement Term,Direction,Response Type,Event Type (Violation or Warning),Lower bound,Upper bound,Parameters
CPUTot,EQ,PAUSE_APP
IDLE_HOST+ACCELERATED,EQ,UNPAUSE_APP
cpu-measured,GT,PAUSE_APP
HOST:ns50:cpu-measured,GT,PAUSE_APP
APP_FINISHED,EQ,KILL_SIMILAR_APPS,WARNING,0,0,application=RK-Bench-Test
APP_FINISHED,EQ,KILL_SIMILAR_APPS,WARNING,0,0,application=COMPSs
app_power:RK-Bench-Test:*,GT,HARD_KILL_APP
app_power:RK-Bench-Test:*,GT,PAUSE_APP,SLA_BREACH,-10000,10000,application=RK-Bench-Test;UNPAUSE=10
!app_power:RK-Bench-Test:*,EQ,UNPAUSE_APP,WARNING
cron_test,EQ,HARD_KILL_APP,WARNING,0,0,application=RK-Bench-Test,START_TIME=9:00;END_TIME=17:00;DAY_OF_WEEK=1111100
```

The additional parameters are: Event Type (Violation or Warning), Lower bound, Upper bound and Parameters. These additional parameters allow for the prospect of having several rules with similar initial parameters of: agreement term, direction, but dependent upon the scale of the violation may have different adaptation responses. 

Event Type (Violation or Warning): This can take the value of violation, warning or other. This means if a warning event arrives indicating that a breach may occur soon the SAM can take pre-emptive action. It also means in an information event arrives, indicating the environment is going into a known period of low load the SAM can scale down the application and use less VMs to meet the demand from the lower workload.

The Lower and Upper bound, values indicate how far the guaranteed value should be away from the actual measured value before the rule fires. This allows the rules to be stacked so the highest priority rules are at the top. Thus when the boundary conditions for each rule are met the first rule found fires. In the event that rule doesn’t work the alternative rules afterwards may then fire instead.

In this case the “SCALE_TO_N_TASKS” response type is used and count of tasks (TASK_COUNT=1) to specify as parameters is used as well.

Application based events, such as APP_FINISHED, may have in the rules file the parameter specified "application=MY_APP", so that rules only fire when the named application finishes. A valid example of such a rule is:

```
APP_FINISHED,EQ,KILL_SIMILAR_APPS,WARNING,0,0,application=RK-Bench
```
It is possible once a pause action has been invoked for a timed unpause to take place. This can be seen in the example below. The second line specifies the final action to take once the 1 hour (3600 second) timer has expired. 

```
app_power:RK-Bench-Test:*,GT,PAUSE_APP,SLA_BREACH,-10000,10000,application=RK-Bench-Test;UNPAUSE=3600
!app_power:RK-Bench-Test:*,EQ,UNPAUSE_APP,WARNING
```

The command MINIMIZE_WALL_TIME_SIMILAR_APPS allows for the ability to shrink the walltime of an application, so that the packing of applications during backfilling is performed better. The default is to shrink the wall time to the average + 10% though the factor of slack may be adjusted using a parameter SLACK_FACTOR. This can be seen below.

```
APP_STARTED,EQ,MINIMIZE_WALL_TIME_SIMILAR_APPS,WARNING,0,0,application=RK-Bench-Test;SLACK_FACTOR=1.20
```

The response type SET_POWER_CAP requires a parameter to be set called POWER_CAP. An example of this being seen below:

```
START_OF_DAY,EQ,SET_POWER_CAP,WARNING,0,0,POWER_CAP=1000
```

This command can be tied to a CronEvent such as: 1,START_OF_DAY,0 0 08 ? * MON-FRI *. Triggering a start of day event, this therefore allows the power cap to be adjusted to schedule during the day.

The REDUCE_POWER_CAP, INCREASE_POWER_CAP increment and decrement power by 10W by default. This can be changed by adding the parameter POWER_INCREMENT, such as POWER_INCREMENT=100. These commands together therefore allow for greater flexibilty in adjusting the power cap during the day.

The command RESELECT_ACCELERATORS allows for the ability to redeploy the application and use different accelerators. It has the parameters,RANK_BY and KILL_PREVIOUS. RANK_BY allows an applictions configuration options to be ranked by either ENERGY or TIME. The default of which is to rank by energy. The second parameter KILL_PREVIOUS is either true or false and indicates if the previous job should be cancelled or not. The default is false.

All rules may have time constraints specified as parameters, these cover aspects such as the START_TIME, END_TIME and DAY_OF_WEEK, to which the rule is applicable. An example of these parameters are given below, which restrict a rule to Monday to Friday, 9am to 5pm:

```
START_TIME=9:00;END_TIME=17:00;DAY_OF_WEEK=1111100
```

## Usage Guide

The self-adaptation manager runs as a service waiting for events to occur on the infrastructure. i.e. through SLURM or CollectD based events. This service is launched by executing the jar, which is simply executed with the command: 

```
java -jar self-adaptation-manager.jar
```

An alternative to listening for events from SLURM or CollectD is to receive events through a REST based interface. This allows for events to be submitted via the following curl command:

```
curl -H "Content-type: application/json" -d '{
         "origin" : "Host",
         "rawValue" : 0,
         "guaranteedValue" : 0,
         "type" : "WARNING",
         "guaranteeOperator" : "EQ",
         "agreementTerm" : "IDLE_HOST",
         "guaranteeid" : "IDLE_HOST",
         "hostname" : "ns51",
         "applicationId" : "",    
         "deploymentId" : ""
         }' 'http://localhost:8080/sam/event/submit'
```

In the example above it submits a event warning of an idle host ns51. The value for origin can be either "Host" or "Application", which create events of a given type. 

The WADL for the SAMs REST interface can be found using the command: ```curl http://localhost:8080/sam/application.wadl``` and additionally a liveliness test can be used by the following command: ```curl http://localhost:8080/sam/event/test```.

## Relation to other TANGO components

The self-adaptation manager works with: 

* **ALDE** - The ALDE will provide interfaces for the Self-Adaptation Manager to change the configuration of an application to optimize its execution in a TANGO compatible testbed.
* **Device Supervisor** - The SAM can directly interface with the device supervisor as a means of using it as both a datasource for monitoring the environment and for invoking adaptation.
* **Energy Modeller** - The energy modeller provides guidance to the self-adaptation manager on how much power is being consumed by an application. It also allows for it to determine the effect on power consumption of proposed changes to the applications configuration.
* **Monitoring Infrastructure** - The SAM can interface with the monitoring infrastructure as a means of using it as a datasource for monitoring the environment.
