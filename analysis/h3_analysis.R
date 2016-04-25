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

data.20 <- read.table('../output/end_count.2016.Apr.24.19_15_59.txt', header=TRUE, sep=',')
data.other <- read.table('../output/end_count.2016.Apr.24.17_11_43.txt', header=TRUE, sep=',')
efficiency.20 <- efficiencyFromData(data.20)
efficiency.other <- efficiencyFromData(data.other)

survival <- c(efficiency.20["efficiency.t"], efficiency.other["efficiency.t"])
names(survival) <- c("1:20", "Other")
boxplot(survival)
title(main="Survival Rate", ylab="Survial Rate")

time <- c(efficiency.20["time"], efficiency.other["time"])
names(time) <- c("1:20", "Other")
boxplot(time)
title(main="Evacuation Time", ylab="Ticks")
