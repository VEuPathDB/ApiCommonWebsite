## server.r
#options(shiny.trace=TRUE)
require(rCharts)

source("../../lib/wdkDataset.R")
source("config.R")

shinyServer(function(input, output, session) {

  datasetFetcher <- reactive(
    getWdkDataset(session, fetchStyle, FALSE, dataStorageDir)
  )
  
  output$myChart <- renderChart({

    ps <- datasetFetcher()
    names(ps) <- substr(names(ps),2,50) # Drop the preceeding 'x's added to the variable names
    names(ps) <- gsub('\\.','',names(ps)) # Drop periods inserted into column names by R

    #coerce variables in the data frame to desired datatype
    ps$Sex <- as.factor(ps$Sex)
    ps$G6pdGenotype <- as.factor(ps$G6pdGenotype)
    ps$HbsGenotype <- as.factor(ps$HbsGenotype)
    ps$AthalassemiaGenotype <- as.factor(ps$AthalassemiaGenotype)
    ps$GeographicLocation <- as.factor(ps$GeographicLocation)

    str(ps) # Check data after conversion
    
    # compute max dates
    #max.d <- max(as.Date(cv$visit_date,"%Y-%m-%d"))
    #min.d <- min(as.Date(cv$visit_date,"%Y-%m-%d"))

    #str(input)

    # Set variable as supplied from the UI
    x1 <- input$x
    #if (is.na(x1)) { x1 <- "AvgHemoglobin" }
    y1 <- input$y
    #if (is.na(y1)) { y1 <- "Avgageatvisit" }
    clr <- input$color
    #if (is.na(clr)) { clr <- "G6pdGenotype" }
    facet <- input$facet
    #if (is.na(facet)) { facet <- "GeographicLocation" }
    plt <- input$ptype
    #if (is.na(plt)) { plt <- "point" }

    #Plot out graph using supplied information
    p1 <- rPlot(x1, y1, data=ps, type=plt, color=clr, facet=facet)
    p1$addParams(dom = 'myChart')
    return(p1)

  })
})