# HospitalEvacuation
## Bits Please

### Running Instructions
This project is a Repast model to simulate doctors and patients evacuating a hospital during the event of a disaster, ex. a gas leak. You must have Repast Simphony installed to run the simulation. To run the project, load the `HospitalEvacuation` project into Eclipse with the Repast Simphony tools. Choose the `HosipitalEvacuation Model` launch target when running the project - the Repast Simphony GUI will appear. Press the play button to run the simulation with the default settings. The simulation will end automatically when all of the human agents have either exited the hospital or died.

### Space Respresentation
The hospital environment is displayed as a rectangular bordered in blue. Along the borders, there are 5 doors placed (represented as Xs), which serve as goals for the agents in the system. The doctors are modeled as green arrows while the patients are yellow circles. The spreading gas is represented as a light blue cluster.

### Other Information
A variety of parameters can be set from within the GUI. Namely, the number of doctors and patients and the means and standard deviations for the doctor charisma and starting patient panic levels. These parameters allow you to run the simulation under a variety of conditions and will allow us to adjust the independent variables in our experiements.

While we have not yet implemented the logging or batch configurations that will allow us to effectively run our trials, there are two charts included in the GUI. The first is the number of gas particles over time, which allows us to see the spread rate of the gas. The second charts the number of agents remaining in the simulation, by type. It plots the number of living doctors, living patients, dead doctors, and dead patients. This graph gives some insights into the efficiency of the evacuation as the number of doctors/patients will decrease as they exit the simulation or are poisoned and the number of dead doctors/patients will increase as they are poisoned.
