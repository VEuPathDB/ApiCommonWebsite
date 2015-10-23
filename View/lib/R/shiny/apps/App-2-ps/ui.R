## ui.R
require(rCharts)
options(RCHART_LIB = 'polycharts')

shinyUI(pageWithSidebar(
  headerPanel("Visualizing participant information"),
  
  sidebarPanel(
    selectInput(inputId = "x",
                label = "Plot",
                choices = c('AvgHemoglobin', 'AvgWeight', 'AvgAnopheles', 'Avgageatvisit','AgeAtTimeOfEnrollment',
                            'GeoMeanParasiteDensity','MatchingVisitsYear','matchingvisits','YearsofObservation'),
                selected = "AvgHemoglobin"),
    selectInput(inputId = "y",
                label = "Against",
                choices = c('Avgageatvisit','AvgHemoglobin', 'AvgWeight', 'AvgAnopheles', 'AgeAtTimeOfEnrollment',
                            'GeoMeanParasiteDensity','MatchingVisitsYear','matchingvisits','YearsofObservation'),
                selected = "Avgageatvisit"),
    selectInput(inputId = "facet",
                label = "Facets",
                choices = c('GeographicLocation','Sex', 'G6pdGenotype','AthalassemiaGenotype','HbsGenotype'),
                selected = "GeographicLocation"),
    selectInput(inputId = "color",
                label = "Color",
                choices = c('G6pdGenotype','GeographicLocation','Sex', 'AthalassemiaGenotype','HbsGenotype'),
                selected = "G6pdGenotype"),
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