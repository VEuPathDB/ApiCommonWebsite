## ui.R
require(rCharts)
shinyUI(pageWithSidebar(
  headerPanel("Clinical Visit data Visualizations"),
  
  sidebarPanel(
    selectInput(inputId = "x",
                label = "Plot",
                choices = c('AgeAtTimeOfVisit','visit_date','Hemoglobin','AsexualParasiteDensity'),
                selected = "AgeAtTimeOfVisit"),
    selectInput(inputId = "y",
                label = "Against",
                choices = c('Hemoglobin','AsexualParasiteDensity','AgeAtTimeOfVisit'),
                            selected = "Hemoglobin"),
    selectInput(inputId = "color",
                label = "Color",
                choices = c('MalariaDiagnosis','GeographicLocation', 'BloodSmearStatus', 
                            'ClinicalVisitType'),
                selected = "BloodSmearStatus"),
#    selectInput(inputId = "group",
#                label = "Group",
#                choices = c('GeographicLocation', 'MalariaDiagnosis', 'BloodSmearStatus', 
#                            'ClinicalVisitType'),
#                selected = ""),
#    selectInput(inputId = "fill",
#                label = "Fill",
#                choices = c( 'BloodSmearStatus', 'GeographicLocation', 'MalariaDiagnosis',
#                            'ClinicalVisitType'),
#                selected = ""),
    selectInput(inputId = "facet",
                label = "Facet",
                choices = c('GeographicLocation', 'ClinicalVisitType', 'BloodSmearStatus'),
                selected = "GeographicLocation"),
    selectInput(inputId = "type",
                label = "Graph Type",
                choices = c('point', 'bar'),
                selected = "point")
  ),
  mainPanel(
    showOutput("myChart", "polycharts")
  )
))
