## ui.R
require(rCharts)
shinyUI(pageWithSidebar(
  headerPanel("Clinical Visit data Visualizations"),
  
  sidebarPanel(
    selectInput(inputId = "x",
                label = "Plot",
                choices = c('AgeAtTimeOfVisit',  
                            'Hemoglobin','AsexualParasiteDensity'),
                selected = "AgeAtTimeOfVisit"),
    selectInput(inputId = "y",
                label = "Against",
                choices = c('Hemoglobin', 'AsexualParasiteDensity','AgeAtTimeOfVisit'),
                            selected = "AsexualParasiteDensity"),
    selectInput(inputId = "color",
                label = "Color",
                choices = c('MalariaDiagnosis','SevereMalariaSymptoms','GeographicLocation', 'BloodSmearStatus', 
                            'Species','ClinicalVisitType'),
                selected = ""),
    selectInput(inputId = "group",
                label = "Group",
                choices = c('GeographicLocation', 'MalariaDiagnosis', 'BloodSmearStatus', 
                            'SevereMalariaSymptoms','Species','ClinicalVisitType'),
                selected = ""),
    selectInput(inputId = "fill",
                label = "Fill",
                choices = c( 'BloodSmearStatus', 'GeographicLocation', 'MalariaDiagnosis',
                            'SevereMalariaSymptoms','Species','ClinicalVisitType'),
                selected = ""),
    selectInput(inputId = "facet",
                label = "Facet",
                choices = c('GeographicLocation', 'MalariaDiagnosis', 'BloodSmearStatus', 
                            'SevereMalariaSymptoms'),
                selected = ""),
    selectInput(inputId = "type",
                label = "Graph Type",
                choices = c('point', 'bar'),
                selected = "")
  ),
  mainPanel(
    showOutput("myChart", "polycharts")
  )
))