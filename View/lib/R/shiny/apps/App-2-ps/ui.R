## ui.R
require(rCharts)
#options(RCHART_LIB = 'polycharts')

shinyUI(pageWithSidebar(
  headerPanel("Visualizing participant information"),
  
  sidebarPanel(
    selectInput(inputId = "x",
                label = "Plot",
                choices = c('AvgHemoglobin', 'AvgWeight','AvgAnopheles','Avgageatvisit','AgeAtTimeOfEnrollment',
                            'GeoMeanParasiteDensity','MatchingVisitsYear','matchingvisits','YearsofObservation',
                            'TotalWithAsymptomaticParasitemiaPerYear','VisitsWithMalariaPerYear',
                            'AveParasiteDensityasymptomaticParasitemia','AveParasiteDensitymalariaDiagnosis'),
                selected = "Avgageatvisit"),
    selectInput(inputId = "y",
                label = "Against",
                choices = c('Avgageatvisit','AvgHemoglobin','AvgWeight','AvgAnopheles','AgeAtTimeOfEnrollment',
                            'GeoMeanParasiteDensity','MatchingVisitsYear','matchingvisits','YearsofObservation',
                            'TotalWithAsymptomaticParasitemiaPerYear','VisitsWithMalariaPerYear',
                            'AveParasiteDensityasymptomaticParasitemia','AveParasiteDensitymalariaDiagnosis'),
                selected = "MatchingVisitsYear"),
    selectInput(inputId = "facet",
                label = "Facets",
                choices = c('SubcountyInUganda','Sex','G6pdGenotype','AthalassemiaGenotype','HbsGenotype'),
                selected = "SubcountyInUganda"),
    selectInput(inputId = "color",
                label = "Color",
                choices = c('SubcountyInUganda','Sex','G6pdGenotype','AthalassemiaGenotype','HbsGenotype'),
                selected = "HbsGenotype"),
    selectInput(inputId = "ptype",
                label = "Plot Type",
                choices = c('point', 'bar'),
                selected = "point")
    #dateRangeInput("dateR","Period", start = min.d, end = max.d, min = min.d, max =max.d)
  ),
  mainPanel(
    showOutput("myChart", "polycharts")
  )
))
