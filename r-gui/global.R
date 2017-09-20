# load csv here
maxDuration <- 700

millisecondsAsDays <- function(millis) {
  return (millis / 1000 / 60 / 60 / 24)
}

daysAsSeconds <- function(days) {
  return (days * 60 * 60 * 24)
}

floor_dec <- function(x, level=1) round(x - 5*10^(-level-1), level)
ceiling_dec <- function(x, level=1) round(x + 5*10^(-level-1), level)

durations <- read.csv("../duration_histogram.csv", header = TRUE)
variables <- read.csv("../variable_histogram.csv", header = TRUE)


durationInDays <- sapply(durations$duration, millisecondsAsDays)
durations <- cbind(durations, durationInDays)

activityIds <- unique(durations$activityId)

variableNames <- unique(variables$variableName)
