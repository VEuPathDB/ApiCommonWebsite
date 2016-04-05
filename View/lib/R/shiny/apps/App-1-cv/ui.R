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
                choices = c('MalariaDiagnosis','SubcountyInUganda', 'AsexualParasitesPresent', 
                            'VisitType'),
                selected = "MalariaDiagnosis"),
#    selectInput(inputId = "group",
#                label = "Group",
#                choices = c('SubcountyInUganda', 'MalariaDiagnosis', 'AsexualParasitesPresent', 
#                            'VisitType'),
#                selected = ""),
#    selectInput(inputId = "fill",
#                label = "Fill",
#                choices = c( 'AsexualParasitesPresent', 'SubcountyInUganda', 'MalariaDiagnosis',
#                            'VisitType'),
#                selected = ""),
    selectInput(inputId = "facet",
                label = "Facet",
                choices = c('SubcountyInUganda', 'VisitType', 'AsexualParasitesPresent'),
                selected = "SubcountyInUganda"),
    selectInput(inputId = "type",
                label = "Graph Type",
                choices = c('point', 'bar'),
                selected = "point")
  ),
  mainPanel(
    showOutput("myChart", "polycharts")
  )
))
