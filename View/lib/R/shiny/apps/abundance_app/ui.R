library(shiny)

shinyUI( 
	fluidPage(
	  selectInput("taxonLevel", label = "Level: ", 
	              choices = c("Phylum", "Class", "Order", "Family", "Genus", "Species"), selected = "Phylum"),
	  div(style = "display: none;",
	      checkboxInput("taxa_are_rows", label = "", value = T)),
	  uiOutput("category"),
    		div(
    			style = "position:relative",
    			plotOutput("abundanceChart",
    								 hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
    								 click = clickOpts("plot_click"), width = "100%"),
    			uiOutput("hover_info")
    			# dataTableOutput("sample_subset")
    		)
) 
	
) # end shinyUI