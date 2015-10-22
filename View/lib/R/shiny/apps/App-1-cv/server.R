## server.r
require(rCharts)
shinyServer(function(input, output) {
  
    output$myChart <- renderChart({
      #Set values for graphing
      x_ <- input$x
      y_ <- input$y
      clr <- input$color
      fct <- input$facet
      fill <- input$fill
      grp <- input$group
      typ <- input$type
      sdate <- input$dateR[1]
      edate <- input$dateR[2]
      
      #scv <- subset(cv, visit_date > min.d & visit_date < max.d, select = c(x_, y_, clr,fill,grp,visit_date))
      #paste(scv)
    p1 <- rPlot(input$x, input$y, data = cv, type = typ, group =grp,facet=fct, color=clr)
    p1$addParams(dom = 'myChart')
    return(p1)
  })
})