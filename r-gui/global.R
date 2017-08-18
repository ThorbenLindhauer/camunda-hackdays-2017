# load csv here
maxDuration <- 700

durations <- read.csv("../../../data/duration_histogram.csv", header = TRUE)
variables <- read.csv("../../../data/variable_histogram.csv", header = TRUE)

durations$duration <- durations$duration / 1000

activityIds <- unique(durations$activityId)

variableNames <- unique(variables$variableName)