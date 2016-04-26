efficiencyFromData <- function(data) {
  odds <- seq(1, nrow(data), 2)
  evens <- seq(2, nrow(data), 2)
  
  time <- data[evens, "tick"]
  efficiency.d <- (data[odds, "doctor_count"] - data[evens, "dead_doctor_count"]) / data[odds, "doctor_count"]
  efficiency.p <- (data[odds, "patient_count"] - data[evens, "dead_patient_count"]) / data[odds, "patient_count"]
  efficiency.t <- (data[odds, "doctor_count"] + data[odds, "patient_count"] - data[evens, "dead_doctor_count"] - data[evens, "dead_patient_count"]) / (data[odds, "doctor_count"] + data[odds, "patient_count"])
  
  efficiency <- data.frame(time, efficiency.d, efficiency.p, efficiency.t)
  return(efficiency)
}

data.20 <- read.table('../output/h3_1_20.txt', header=TRUE, sep=',')
efficiency.20 <- efficiencyFromData(data.20)

data.15 <- read.table('../output/h3_1_15.txt', header=TRUE, sep=',')
efficiency.15 <- efficiencyFromData(data.15)

data.10 <- read.table('../output/h3_1_10.txt', header=TRUE, sep=',')
efficiency.10 <- efficiencyFromData(data.10)

data.5 <- read.table('../output/h3_1_5.txt', header=TRUE, sep=',')
efficiency.5 <- efficiencyFromData(data.5)

data.1 <- read.table('../output/h3_1_1.txt', header=TRUE, sep=',')
efficiency.1 <- efficiencyFromData(data.1)

survival <- c(efficiency.20["efficiency.t"], efficiency.15["efficiency.t"], efficiency.10["efficiency.t"], efficiency.5["efficiency.t"], efficiency.1["efficiency.t"])
names(survival) <- c("1:20", "1:15", "1:10", "1:5", "1:1")
boxplot(survival)
title(main="Hypothesis 3: Survival Rate", ylab="Survival Rate")

survival.d <- c(efficiency.20["efficiency.d"], efficiency.15["efficiency.d"], efficiency.10["efficiency.d"], efficiency.5["efficiency.d"], efficiency.1["efficiency.d"])
names(survival.d) <- c("1:20", "1:15", "1:10", "1:5", "1:1")
boxplot(survival.d)
title(main="Hypothesis 3: Survival Rate (Doctors)", ylab="Survival Rate")

survival.p <- c(efficiency.20["efficiency.p"], efficiency.15["efficiency.p"], efficiency.10["efficiency.p"], efficiency.5["efficiency.p"], efficiency.1["efficiency.p"])
names(survival.p) <- c("1:20", "1:15", "1:10", "1:5", "1:1")
boxplot(survival.p)
title(main="Hypothesis 3: Survival Rate (Patients)", ylab="Survival Rate")

time <- c(efficiency.20["time"], efficiency.15["time"], efficiency.10["time"], efficiency.5["time"], efficiency.1["time"])
names(time) <- c("1:20", "1:15", "1:10", "1:5", "1:1")
boxplot(time)
title(main="Hypothesis 3: Evacuation Time", ylab="Ticks")

plot(efficiency.20$efficiency.p, efficiency.20$efficiency.d, type='p', main="Hypothesis 3: Patient vs Doctor Survival Rate (1:20)", xlab="Patient Survival Rate", ylab="Doctor Survival Rate")
