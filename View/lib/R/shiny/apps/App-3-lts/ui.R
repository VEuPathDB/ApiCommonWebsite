#Declare Shiny UI
require(rCharts)
shinyUI(
  #pageWithSidebar, has three components - head, side, main panels
  pageWithSidebar(
    headerPanel("Light Trap Visualizations"),
    sidebarPanel(
      #specify options
      selectInput(inputId="pvar" ,label="Plot",choices = c("AnophelesCollectedtotal","TotalAnophelesPositive","TotalAnophelesTested","Parous",
                                         "Nulliparous","AFunestus","AGambiae","OtherAnopheles")),
      selectInput(inputId="yvar", label="against", choices = ("DateOfVisit")),
#      selectInput(inputId="facet",label="Facet",choices = ("GeographicLocation"), selected = ""),
      selectInput(inputId="group",label="Group", choices = ("GeographicLocation"), selected = "")
    ),
    mainPanel(
      showOutput("LTSChart","polycharts")
    )
  )
)