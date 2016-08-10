#Declare Shiny UI
require(rCharts)
shinyUI(
  #pageWithSidebar, has three components - head, side, main panels
  pageWithSidebar(
    headerPanel("Light Trap Visualizations"),
    sidebarPanel(
      #specify options
      selectInput(inputId="pvar" ,label="Plot",choices = c("TotalAnophelesPositive","TotalAnophelesTested","Parous", "Nulliparous")),
      selectInput(inputId="yvar", label="against", choices = ("DateOfVisit")),
#      selectInput(inputId="facet",label="Facet",choices = ("SubcountyInUganda"), selected = ""),
      selectInput(inputId="group",label="Group", choices = ("SubcountyInUganda"), selected = "")
    ),
    mainPanel(
      showOutput("LTSChart","polycharts")
    )
  )
)
