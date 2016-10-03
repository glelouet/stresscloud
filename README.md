# Stresscloud

Stresscloud is a distributed resource stress manager.

It provides execution of a distributed test scenario to registered stressers on the network.

Once a scenario is performed, you can retrieve performance values

# Scenario

The scenario are written in groovy
 - to provide smaller scenarios thanks to collections operators
 - to have an easy interpreter

An example of scenario is found in [the common resources](StressCloud-common/src/main/resources)

A scenario is composed of three parts : selection of the stressers, scheduling of the stress, analyzis of the result

## Selection of the stressers

The Stresscloud works on a client-server principle.
In a scenario, the server is called [registar](StressCloud-API/src/main/java/fr/lelouet/stresscloud/control/VMRegistar.java) and we request him the VM needed .

First, we always *need* a given number of VM to be present BEFORE selecting them. This is for deterministic feature : we want to always select the same VM in the same scenario. So we must always have the same number of VM to start a scenario, if a VM takes longer to start it must not prevent the same scenario from executing again.

Then we can *require* and assign groups of VM.

## Load scheduling

see the scenario examples. Some available instructions are stresser-specific, but basically we can do things like 
 - add a total load to execute , eg send 10MB to a VM or write 1GB to the disk.
 - try to execute given number of operations every second.
 - synchronize the scenario with time.
 - synchronize the VMs with their stresser activity.

## Stress result analysis
 todo later
