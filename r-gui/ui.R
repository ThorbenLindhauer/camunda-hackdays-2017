library(shiny)

shinyUI(fluidPage(

  titlePanel("Outlier Analysis"),

  mainPanel(
    selectInput("activity", "Activity Id:", activityIds),
    plotOutput("duration_histogram"),
    sliderInput("duration", "Duration (Days): ", min=0, max=100, value = c(0,100), width='100%', step=0.1, round=-2),
    selectInput("variable", "Variable Name:", variableNames),
    plotOutput("variable_histogram"),
    width=12
  )
))
