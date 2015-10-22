library(zoo)
shinyServer(
  function(input,output){
      output$LTSChart <-renderChart({
      x <-input$pvar
      y <- input$yvar
      g <- input$group
      f <- input$facet
     
      #sort dataframe by MonthYear
      d <- LTS.D[order(as.yearmon(LTS.D$DateOfVisit, "%y-%b")),]
      p1 <- rPlot(y, x, data = d, type = 'bar', color ='GeographicLocation', group='GeographicLocation' ) #Plot PD as it depends on MonthYear
      #p1$guides(x = list(title = "", ticks = list(ym)))
      p1$addParams(dom = 'LTSChart')
      return(p1)
    })
  }
)