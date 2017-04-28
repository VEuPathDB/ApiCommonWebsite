# Declaring the packages
library(shiny)

# The shiny ui function build the HTML final page showed in the shiny app
shinyUI( 
	fluidPage(
	  fluidRow(
	    column(3, 
	           selectInput("taxonLevel", label = "Taxonomic level: ", 
	                       choices = c("Phylum", "Class", "Order", "Family", "Genus", "Species"), selected = "Phylum", width = '100%'),
	           # this div is not showed, this is just a workaround to load the files in a reactive environment
	           div(style = "display: none;",
	               checkboxInput("taxa_are_rows", label = "", value = T))
	     ),
	     column(9,
	            uiOutput("category")
	     )
	    ),
	  # fluidRow(
	  #   column(12,
	  #   sliderInput("sliderNumberTaxa", label = "Plot N OTUs per sample:",
	  #                           min = 5, max = 20, step = 5, value = 10, width = '100%')
	  #   )
	  # ),
	  fluidRow(
	    div(
	      style = "position:relative",
	      uiOutput("abundanceChart"
	                 # , height = "600px"
	                 ),
	      uiOutput("hover_info")
	    )
	  ),
	  fluidRow(
	    dataTableOutput("sample_subset")
	  )
) 
	
) # end shinyUI