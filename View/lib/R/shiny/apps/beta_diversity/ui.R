library(shiny)
library(shinyjs)

appCSS <- "
#loading-content {
position: absolute;
background: #FFFFFF;
opacity: 0.9;
z-index: 100;
left: 0;
right: 0;
height: 100%;
text-align: center;
color: #858585;
}
"

shinyUI( 
	fluidPage(
	  useShinyjs(),
	  inlineCSS(appCSS),
	  # Loading message
	  div(id = "loading-content",
	      h5("We are preparing the graphical representation..."),
	      img(src = "loading.gif")
	  ),
	  # The main app code goes here
	  hidden(
	    div(
	      id = "app-content",
	      fluidRow(column(
	        4,
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
	        # this div is not showed, this is just a workaround to load the files in a reactive environment
	        div(style = "display: none;",
	            checkboxInput(
	              "taxa_are_rows", label = "", value = T
	            ))
	      ),
	      column(
	        8,
	        selectizeInput(
	          "category",
	          choices = NULL,
	          label = "Split by the category:",
	          options = list(placeholder = 'Loading...'),
	          width = "100%"
	        )
	      )),
	      fluidRow(column(12,
      	  div(
      	    style = "position:relative",
      	    plotOutput("abundanceChart",
      	               hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
      	               click = clickOpts("plot_click"), width = "100%"),
      	    uiOutput("hover_info")
      	    )
	        )
	      )
	    ) # end div id = "app-content",
	  ) # end hidden
	  ) # end fluidPage
) # end shinyUI