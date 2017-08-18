library(shiny)
library(ggplot2)
library(lubridate)

# Define server logic required to plot various variables against mpg
shinyServer(function(input, output, session) {

  valuesByVarName <- function(varName, activity, minDuration = 0, maxDuration = 2147483647) {
    if (is.null(varName)) {
      varName <- "undefined"
    }

    variablesWithName <- subset(variables, variableName == varName)

    processInstancesInDuration <- subset(durations,
      activityId == activity &
      duration >= minDuration &
      duration <= maxDuration)

    joinedValues <- merge(variablesWithName,    processInstancesInDuration, by = "processInstanceId")

    return(joinedValues)
  }

  durationsByActivity <- function(activity) {
    return(durations[durations$activityId == activity, ]$duration)
  }

  output$variable_histogram <- renderPlot({

    allInstanceVals <- valuesByVarName(input$variable, input$activity)
    group <- rep(c('all instances'), each = nrow(allInstanceVals))
    allInstanceVals <- cbind(allInstanceVals, group)

    selectedInstanceVals <- valuesByVarName(input$variable, input$activity, input$duration[1], input$duration[2])
    group <- rep(c('selected instances'), each = nrow(selectedInstanceVals))
    selectedInstanceVals <- cbind(selectedInstanceVals, group)

    allVals = rbind(allInstanceVals, selectedInstanceVals)

    ggplot(allVals, aes(x=variableValue, group=group, fill=as.factor(group))) +
      geom_histogram(data=allVals, stat='count', position="dodge", aes(
      y=c(..count..[..group..==1]/sum(..count..[..group..==1]),
                         ..count..[..group..==2]/sum(..count..[..group..==2]))*100)) +
      ylab('Percentage in Group') +
      geom_text(aes(label = ..count.., y= ..prop..), stat= "count", position = 'dodge', vjust = -.5) +
      ylim(0, 100)
    })

  output$duration_histogram <- renderPlot({
    minDuration <- input$duration[1]
    maxDuration <- input$duration[2]

    activityInstances <- subset(durations,
      activityId == input$activity &
      duration >= minDuration &
      duration <= maxDuration)

    ggplot(activityInstances) +
      geom_histogram(aes(x=duration, fill='red')) +
      scale_x_time()
  })

  observe({
    durations = durationsByActivity(input$activity)

    maxDuration <- max(durations)
    minDuration <- min(durations)

    updateSliderInput(session, "duration", max=maxDuration, min=minDuration, value=c(minDuration, maxDuration))
  })
})
