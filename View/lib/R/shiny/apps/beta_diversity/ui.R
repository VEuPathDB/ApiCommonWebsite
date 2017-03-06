library(shiny)

shinyUI( 
	fluidPage(
	  selectInput("distance", "Distance Method:",
	              c(
	                "Jensen-Shannon Divergence"="jsd",
	                "Jaccard" = "jaccard",
	                "Bray-Curtis" = "bray",
	                "Canberra" = "canberra",
	                "Kulczynski"="kulczynski",
	                "Horn"="horn",
	                "Mountford"="mountford"
	                )),
  	  div(style = "display: none;",
  	      checkboxInput("taxa_are_rows", label = "", value = T)
  	  ),
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