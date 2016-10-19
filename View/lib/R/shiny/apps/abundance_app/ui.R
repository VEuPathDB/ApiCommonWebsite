library(shiny)

shinyUI( 
	bootstrapPage(
	  div(class="container",
	  fluidRow(
	    column(11,
		  br(),
     selectInput(inputId="taxa.level",label="Choose the taxonomic level: ",
                 choices=c("Kingdom/Superkingdom"=3, "Phylum"=4, "Class"=5, "Order"=6,
                           "Family"=7,"Genus"=8,"Species"=9),
                 selected = 2),
		  
    		div(
    			style = "position:relative",
    			plotOutput("abundanceChart",
    								 hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
    								 click = clickOpts("plot_click"), width = "100%"),
    			uiOutput("hover_info"),
    			dataTableOutput("sample_subset")
    		)
		  )
	  ))
	)
) # end shinyUI