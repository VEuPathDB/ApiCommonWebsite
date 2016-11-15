## server.r
require(rCharts)

source("../../lib/wdkDataset.R")
source("config.R")

shinyServer(function(input, output, session) {

  datasetFetcher <- reactive(
    getWdkDataset(session, fetchStyle, FALSE, dataStorageDir)
  )
  
    output$myChart <- renderChart({

      cv <- datasetFetcher()

      str(cv)
      names(cv) <- substr(names(cv),3,50) #Remove preceeding 'x.'
      names(cv) = gsub("\\.", "", names(cv)) #Remove remaining periods

      #coerce variables to required type:
      cv$VisitType <- as.factor(cv$VisitType)
      cv$AsexualParasitesPresent <- as.factor(cv$AsexualParasitesPresentmicroscopy)
      cv$MalariaDiagnosis <- as.factor(cv$MalariaDiagnosisAndParasiteStatus)
      cv$SubcountyInUganda <- as.factor(cv$SubcountyInUganda)
      cv$AsexualParasiteDensity <- as.factor(cv$AsexualParasiteDensity )
      cv$Febrile <- as.factor(cv$Febrile)
##      cv$Feversubjective <- as.factor(cv$Feversubjective)
##      cv$SevereMalariaSymptoms <- as.factor(cv$SevereMalariaSymptoms)
##      cv$Species <- as.factor(cv$Species)

      str(input)

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
