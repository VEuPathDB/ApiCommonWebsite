library(zoo)
require(rCharts)
require(reshape2)
source("../../lib/wdkDataset.R")
source("config.R")

shinyServer(function(input, output, session) {

  datasetFetcher <- reactive({
    getWdkDataset(session, fetchStyle, FALSE, dataStorageDir)
  })

  output$LTSChart <- renderChart2({

    LTS <- datasetFetcher()
 
    names(LTS) <- substr(names(LTS),3,50) #Remove preceeding 'x.'
    names(LTS) = gsub("\\.", "", names(LTS)) #Remove remaining periods
   
#coerce variables to required type:
LTS$GeographicLocation <- as.factor(LTS$GeographicLocation)

LTS.S <- subset(LTS, AnophelesCollectedtotal >0, select = c("DateOfVisit","AnophelesCollectedtotal","GeographicLocation",
                                                            "TotalAnophelesPositive","TotalAnophelesTested","Parous",
                                                            "Nulliparous","AFunestus","AGambiae","OtherAnopheles","CollectionBarcode"))
      LTS.M <- melt(LTS.S, id.vars =c("DateOfVisit","CollectionBarcode","GeographicLocation") )
      LTS.D <- dcast(LTS.M,DateOfVisit+GeographicLocation~variable, fun.aggregate = sum)
      x <-input$pvar
      y <- input$yvar
      g <- input$group
      f <- input$facet
     
      #sort dataframe by MonthYear
      d <- LTS.D[order(as.yearmon(LTS.D$DateOfVisit, "%y-%b")),]
      p1 <- rPlot(y, x, data = d, type = 'bar', color ='GeographicLocation', group='GeographicLocation' ) #Plot PD as it depends on MonthYear
      str(p1)  
   #p1$guides(x = list(title = "", ticks = list(ym)))
      p1$addParams(dom = 'LTSChart')
      return(p1)
    })
  }
)