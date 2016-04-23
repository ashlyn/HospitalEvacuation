# HospitalEvacuation
## Bits Please

### Running Instructions
This project is a Repast model to simulate doctors and patients evacuating a hospital during the event of a disaster, ex. a gas leak. You
must have Repast Simphony installed to run the simulation. To run the project, load the `HospitalEvacuation` project into Eclipse with
the Repast Simphony tools. Choose the `HosipitalEvacuation Model` launch target when running the project - the Repast Simphony GUI will 
appear. If the launcher does not appear, choose the "Organize Favorites" option and then press the add button. Select the 
"HospitalEvacuation Model" and add it to the favorites and then close the menu and select this launch target.
Press the play button to run the simulation with the default settings. The simulation will end automatically when all of the 
human agents have either exited the hospital or died.

### Space Respresentation
The hospital environment is displayed as a rectangular bordered in blue. Along the borders, there are 5 doors placed (represented as Xs),
which serve as goals for the agents in the system. The doctors are modeled as green arrows while the patients are yellow circles. The 
spreading gas is represented as a light blue cluster.

### Other Information
A variety of parameters can be set from within the GUI. Namely, the number of doctors and patients and the means and standard deviations
for the doctor charisma and starting patient panic levels. These parameters allow you to run the simulation under a variety of conditions
and will allow us to adjust the independent variables in our experiements. The list of parameters and their definitions are below.

## Parameters
- Starting Panic: the starting panic level of patients at the beginning of the simulation (between 0 and 1)
- Mean Panic: the average panic level of patients at the beginning of the simulation (between 0 and 1)
- Threshold for overcrowding: number of patients that need to be near a door for the door to be considered 'overcrowded'
- Doctors: number of doctor agents to spawn at the beginning of the simulation
- Standard Deviation Doctor Charisma: the amount of diversity in doctor charisma desired when spawning doctor agents
- Patient Count: number of patient agents to spawn at the beginning of the simualtion
- Standard deviation Patient Panic: the amount of diversity in panic levels at the beginning of the simulation
- Threshold for blocked door: number of gas agents that need to be near a door for the door to be considered 'blocked'
- Patient Panic Weight: used to calculate panic levels, determines how much impact the number of patients and their panic levels should have on the new panic level (between 0 and 1)
- Mean Doctor Charisma: the average doctor charisma at the beginning of the simulation (between 0 and 1)
- Gas Panic Weight: used to calculate panic levels, determines how much impact the number of gas agents in the patient's radius of knowledge should have (between 0 and 1, ideally patient panic weight + gas panic weight + starting panic = 1)
- Door Radius: the radius around a door used to calculate overcrowding and blocked

While we have not yet implemented the logging or batch configurations that will allow us to effectively run our trials, there are two 
charts included in the GUI. The first is the number of gas particles over time, which allows us to see the spread rate of the gas. The 
second charts the number of agents remaining in the simulation, by type. It plots the number of living doctors, living patients, dead 
doctors, and dead patients. This graph gives some insights into the efficiency of the evacuation as the number of doctors/patients will 
decrease as they exit the simulation or are poisoned and the number of dead doctors/patients will increase as they are poisoned.

### Changelog
A few changes have been made to the agent design code since it was last turned in on April 14. Below is a list of changes made and the rationale behind them.

- _Gaussian vs. Uniform distribution for panic and charisma_: Hypothesis 1 was modified to compare test runs varying charisma and panic to base cases which use a 
  uniformly-distributed charisma and panic levels. Boolean parameters to dictate whether the charisma and panic should be uniformly- or gaussian/normally-distributed 
  were added to the UI and the code was changed to appropriately set a doctor's charisma and a patient's starting panic level appropriately.
