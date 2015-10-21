library(shiny)

source("../../lib/wdkDataset.R")
source("config.R")

shinyServer(function(input, output, session) {

  dataSet <- reactive(
    getWdkDataset(session, fetchStyle, dataStorageDir)
  )

  # draw the histogram with the specified number of bins
  output$distPlot <- renderPlot({
    validate(need(input$bins > 0, 'Please choose 1 or more bins.'))
    length <- dataSet()[, 2]
    bins <- seq(min(length), max(length), length.out = input$bins + 1)
    hist(length, breaks = bins, col = 'darkgray', border = 'white')
  })
})
