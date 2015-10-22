## server.r
#options(shiny.trace=TRUE)
require(rCharts)
shinyServer(function(input, output) {
  output$myChart <- renderChart({
    #Set variable as supplied from the UI
    x1 <- input$x
    y1 <- input$y
    clr <- input$color
    facet <- input$facet
    plt <- input$ptype
    
    #Plot out graph using supplied information
    p1 <- rPlot(x1, y1, data = ps,  type = plt, color = clr, facet = facet)
    p1$addParams(dom = 'myChart')
    return(p1)

  })
})