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
   
# columns in the data file
# [# Female Anopheles Collected]  [# A. Gambiae]  [# A. Funestus] [Total Anopheles Tested]        [Total Anopheles Positive]      [Subcounty In Uganda]   [Date Of Visit] [Gravid A. Gambiae]     [Gravid A. Funestus]    [Parous]        [Nulliparous]   
#coerce variables to required type:
LTS$SubcountyInUganda <- as.factor(LTS$SubcountyInUganda)

LTS.S <- subset(LTS, TotalAnophelesTested >0, select = c("DateOfVisit","TotalAnophelesTested","SubcountyInUganda",
                                                            "TotalAnophelesPositive","TotalAnophelesTested","Parous",
                                                            "Nulliparous","GravidAFunestus","GravidAGambiae"))
      LTS.M <- melt(LTS.S, id.vars =c("DateOfVisit","SubcountyInUganda") )
      LTS.D <- dcast(LTS.M,DateOfVisit+SubcountyInUganda~variable, fun.aggregate = sum)
      x <-input$pvar
      y <- input$yvar
      g <- input$group   
      f <- input$facet
     
      #sort dataframe by MonthYear
      d <- LTS.D[order(as.yearmon(LTS.D$DateOfVisit, "%y-%b")),]
      p1 <- rPlot(y, x, data = d, type = 'bar', color ='SubcountyInUganda', group='SubcountyInUganda' ) #Plot PD as it depends on MonthYear
      str(p1)  
   #p1$guides(x = list(title = "", ticks = list(ym)))
      p1$addParams(dom = 'LTSChart')
      return(p1)
    })
  }
)
