# Self Adaptation Manager

&copy; University of Leeds 2017

The Tango Self-Adaptation Manager (SAM) is a component of the European Project TANGO (http://tango-project.eu ).

SAM is distributed under a [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Description

The Self-Adaptation manager is responsible for co-ordinating the adaptive behaviour of the Tango architecture. The main aim of this adaptation is provide low power and energy usage while maintaining quality of service aspects of applications.

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

The SAM is highly configurable and can be configured using configuration files to change its behaviour. The SAM has the following settings files in order to achieve these changes:

*self-adaptation-manager.properties* Holds properties such as which event assessor and decision engine to use.  
*self-adaptation-manager-threshold.properties* -	This holds settings regarding thresholds before a response occurs.  
*QoSEventCriteria.csv* - This specify the QoS rules that are required to create events
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

Monitoring events are assessed using an event assessor that is specified using the self.adaptation.manager.event.assessor field. The possible options for this field are: StackedThresholdEventAssessor, ThresholdEventAssessor. Once an event has been assessed to determine the type of action to take if any a decision engine is used to determine the scale and exact position of the adaptation i.e. which VM. The decision engine used is determined by the field: self.adaptation.manager.decision.engine. This can be either: RandomDecisionEngine, LastTaskCreatedDecisionEngine or PowerRankedDecisionEngine.

The field self.adaptation.manager.logging indicates if the EventLog.csv and ResponseLog.csv files should be created. These logs record the arrival of events and the response of the SAM to the events.

#### self-adaptation-manager-threshold.properties

This file specifies the threshold of how many similar events must be seen before the SAM reacts. An example is provided below:

self.adaptation.manager.threshold = 0

#### QoSEventCriteria.csv - Used to specify the QoS rules that are required to create Events

This file specifies the basic thresholds that invoke QoS events that apply to all applications and to the infrastructure.

The file has the following parameters that should be added per each line:

* Unique Id - A number that identifies the rule
* Agreement Term - The value that is to be monitored, such as: 
* Comparator - Indicating values such as  LT, LTE, EQ, GT, and GTE
* Event Type - Indicating on the QoS rule firing if it generates an SLA_BREACH or a WARNING
* Guarantee Value - The threshold for the guarantee

The agreement term can be more complex than most. It can simply be a term that the monitoring infrastructure reports to the self-adaptation manager. The list of these recongnised terms is printed out in the file when the application first runs: RecognisedTerms.csv.

It is possible to limit these QoS metrics to work against only a single host, following the format:

```
HOST:<hostname>:<metric>
```

such as: 

```
HOST:ns32:power
```

It can also be values such as: IDLE_HOST, IDLE_HOST+PENDING_JOB, APP_FINISHED or CLOSE_TO_DEADLINE in the case of event generated by monitoring the infrastructure manager. The last for that can be used specifies application based events, in which the following pattern must be met. 

```
APP:<APP_NAME>:<DEPLOYMENT_ID>:<METRIC>:[HOST_OPTIONAL]
```

#### rules.csv – Used for the Threshold Event Assessor and Stacked Threshold Event Assessor

This file works in conjunction with the self-adaptation-manager-threshold.properties file. It specifies some basic rules, for adaptation to occur. There are three fields that must exist and are common to both StackedThresholdEventAssessor and the ThresholdEventAssessor. These are: the agreement term, the direction and the response type.

```
Agreement Term	Direction	Response Type
energy_usage_per_app	LT	REMOVE_TASK
power_usage_per_app	LT	REMOVE_TASK
energy_usage_per_app	LTE	REMOVE_TASK
power_usage_per_app	LTE	REMOVE_TASK
energy_usage_per_app	GT	ADD_TASK
power_usage_per_app	GT	ADD_TASK
energy_usage_per_app	GTE	ADD_TASK
power_usage_per_app	GTE	ADD_TASK
```

The first is the agreement term. This can either be any value that is monitored by the infrastructure. In addition it can be based upon the following events:

* IDLE_HOST - This creates an event in cases where a host becomes idle.
* IDLE_HOST+PENDING_JOB - This creates an event in cases where a host becomes idle and in addition has an accelerator that could be used.
* APP_FINISHED - This event indicates that a job has finished
* CLOSE_TO_DEADLINE - This event indicates a job is close to its deadline, (where it will be ejected from the infrastructure).

The next field is the direction, this indicates if the breach value is higher or lower than the metric value specified. The possible values are LT, LTE, EQ, GT, and GTE. I.e. less than, less than or equal, equals, greater than and greater than or equal to. 

The final field indicates the response type. The possible values are:  

```
INCREASE_WALL_TIME, REDUCE_WALL_TIME, 
ADD_TASK, REMOVE_TASK, SCALE_TO_N_TASKS, 
ADD_CPU, REMOVE_CPU, 
ADD_MEMORY, REMOVE_MEMORY, 
PAUSE_APP, UNPAUSE_APP, 
KILL_APP, HARD_KILL_APP, 
RESELECT_ACCELERATORS, 
REDUCE_POWER_CAP, INCREASE_POWER_CAP, 
SHUTDOWN_HOST, STARTUP_HOST.
```

The Stacked Threshold Event assessor has additional optional arguments, which allow for more complex behaviour.

```
Agreement Term	Comparator	Response Type	Event Type (Violation or Warning)	Lower bound	Upper bound	Parameters
energy_usage_per_app	GT	REMOVE_TASK	 	 	 	 
power_usage_per_app	GT	REMOVE_TASK	 	 	 	 
energy_usage_per_app	GTE	REMOVE_TASK	 	 	 	 
power_usage_per_app	GTE	REMOVE_TASK	 	 	 	 
lower_load_period	EQ	SCALE_TO_N_TASKS	Other	0	1000	TASK_COUNT=1
higher_load_period	EQ	SCALE_TO_N_TASKS	Other	0	1000	TASK_COUNT=3
lower_load_period	EQ	SCALE_TO_N_TASK	violation	0	1000	TASK_COUNT=1
higher_load_period	EQ	SCALE_TO_N_TASKS	violation	0	1000	TASK_COUNT=3
```

The additional parameters are: Event Type (Violation or Warning), Lower bound, Upper bound and Parameters. These additional parameters allow for the prospect of having several rules with similar initial parameters of: agreement term, direction, but dependent upon the scale of the violation may have different adaptation responses. 

Event Type (Violation or Warning): This can take the value of violation, warning or other. This means if a warning event arrives indicating that a breach may occur soon the SAM can take pre-emptive action. It also means in an information event arrives, indicating the environment is going into a known period of low load the SAM can scale down the application and use less VMs to meet the demand from the lower workload.

The Lower and Upper bound, values indicate how far the guaranteed value should be away from the actual measured value before the rule fires. This allows the rules to be stacked so the highest priority rules are at the top. Thus when the boundary conditions for each rule are met the first rule found fires. In the event that rule doesn’t work the alternative rules afterwards may then fire instead.

In this case the “SCALE_TO_N_TASKS” response type is used and count of tasks (TASK_COUNT=1) to specify as parameters is used as well.

## Usage Guide

The jar is simply executed with the command: 

```
java -jar self-adaptation-manager.jar
```

## Relation to other TANGO components

The self-adaptation manager works with: 

* **ALDE** -  The ALDE will provide interfaces for the Self-Adaptation Manager to change the configuration of an application to optimize its execution in a TANGO compatible testbed.
* **Device Supervisor** - The SAM can directly interface with the device supervisor as a means of using it as both a datasource for monitoring the environment and for invoking adaptation.
* **Energy Modeller** - The energy modeller provides guidance to the self-adaptation manager on how much power is being consumed by an application. It also allows for it to determine the effect on power consumption of proposed changes to the applications configuration.
* **Monitoring Infrastructure** - The SAM can interface with the monitoring infrastructure as a means of using it as a datasource for monitoring the environment.
