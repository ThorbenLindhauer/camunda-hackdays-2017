library(shiny)

shinyUI(pageWithSidebar(

  headerPanel("Outlier Analysis"),

  sidebarPanel(
  ),

  mainPanel(
    selectInput("activity", "Activity Id:", activityIds),
    plotOutput("duration_histogram"),
    sliderInput("duration", "Duration: ", min=0, max=100, value = c(0,100), width='100%'),
    selectInput("variable", "Variable Name:", variableNames),
    plotOutput("variable_histogram")
  )
))
