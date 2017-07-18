# Self Adaptation Manager

&copy; University of Leeds 2017

The Tango Self-Adaptation Manager (SAM) is a component of the European Project TANGO (http://tango-project.eu ).

SAM is distributed under a [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Description

The Self-Adpatation manager is responsible for co-ordinating the adaptive behaviour of the Tango architecture. The main aim of this adaptation is provide low power and energy usage while maintaining quality of service aspects of applications.

## Installation Guide

This guide it is divided into two different guides, one specific to compilation of the Self Adaptation Manager and the second on how to run and configure the SAM.

### Compilation

#### Requirements

The SAM's primary two prerequisites are:

* Java
* Maven

#### Installation and configuration procedure


#### Build status from Travis-CI

[![Build Status](https://travis-ci.org/TANGO-Project/self-adaptation-manager.svg?branch=master)](https://travis-ci.org/TANGO-Project/self-adaptation-manager)

#### Sonar Cloud reports:
The Sonar Cloud reports for this project are available at: https://sonarcloud.io/dashboard?id=eu.tango%3Aself-adaptation-manager

### Installation for running the service

In this case, we are going to detail how to run the application so that it can manage the adaptation in a Tango compliant environment.

#### Configuring the service

TODO

## Usage Guide

TODO

## Relation to other TANGO components

The self-adaptation manager works with: 

* **ALDE** -  The ALDE will provide interfaces for the Self-Adaptation Manager to change the configuration of an application to optimize its execution in a TANGO compatible testbed.
* **Device Supervisor** - The SAM can directly interface with the device supervisor as a means of using it as both a datasource for monitoring the environment and for invoking adaptation.
* **Monitoring Infrastructure** - The SAM can interface with the monitoring infrastructure as a means of using it as a datasource for monitoring the environment..
