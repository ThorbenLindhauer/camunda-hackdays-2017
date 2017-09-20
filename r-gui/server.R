library(shiny)
library(ggplot2)
library(lubridate)
library(dplyr)

# Define server logic required to plot various variables against mpg
shinyServer(function(input, output, session) {

  valuesByVarName <- function(varName, activity, minDurationDays = 0, maxDurationDays = 2147483647) {
    if (is.null(varName)) {
      varName <- "undefined"
    }

    variablesWithName <- subset(variables, variableName == varName)

    processInstancesInDuration <- subset(durations,
      activityId == activity &
      duration >= minDurationDays &
      duration <= maxDurationDays)

    joinedValues <- merge(variablesWithName,    processInstancesInDuration, by = "processInstanceId")

    return(joinedValues)
  }

  durationsByActivity <- function(activity) {
    return(durations[durations$activityId == activity, ])
  }

  output$variable_histogram <- renderPlot({

    allInstanceVals <- valuesByVarName(input$variable, input$activity)
    group <- rep(c('All Instances'), each = nrow(allInstanceVals))
    allInstanceVals <- cbind(allInstanceVals, group)

    selectedInstanceVals <- valuesByVarName(input$variable, input$activity, input$duration[1], input$duration[2])
    group <- rep(c('Selected Instances'), each = nrow(selectedInstanceVals))
    selectedInstanceVals <- cbind(selectedInstanceVals, group)

    allVals = rbind(allInstanceVals, selectedInstanceVals)

    agg <- allVals %>%
      group_by(group, variableValue) %>%
      summarise(freq = n()) %>%
      mutate(percentage = freq * 100 / sum(freq) )

    ggplot(agg, aes(x=variableValue, y=percentage, group=group)) +
      geom_col(aes(fill=group), position='dodge') +
      geom_text(aes(label=freq, y=5), position = position_dodge(0.9)) +
      scale_y_continuous(name="Percentage of Instances") +
      coord_cartesian(ylim=c(0, 100)) +
      scale_x_discrete(name="Variable Value") +
      #labs(colour = "Selection")
      guides(fill=guide_legend(title="Selection"))
    })

  output$duration_histogram <- renderPlot({
    activityInstances <- subset(durations,
      activityId == input$activity &
      durationInDays >= input$duration[1] &
      durationInDays <= input$duration[2])

    ggplot(activityInstances) +
      geom_histogram(show.legend=FALSE, bins=30, aes(x=durationInDays, fill='red')) +
      scale_x_continuous(name="Duration (Days)") +
      scale_y_continuous(name="Number Of Instances")
  })

  observe({
    durations <- durationsByActivity(input$activity)
    durationInDays <- durations$durationInDays

    maxDuration <- ceiling_dec(max(durationInDays), 1)
    minDuration <- floor_dec(min(durationInDays), 1)

    updateSliderInput(session, "duration", max=maxDuration, min=minDuration, value=c(minDuration, maxDuration))
  })
})
