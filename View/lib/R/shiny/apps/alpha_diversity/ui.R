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
	  fluidRow(
	    column(5, 
	           checkboxGroupInput("measureCheckBox", label="Measure(s):",
	                              choices = c("Chao1" = "Chao1",
	                                          "ACE" = "ACE",
	                                          "Shannon" = "Shannon",
	                                          "Simpson" = "Simpson",
	                                          "Fisher" = "Fisher"),
	                              selected = c("Shannon", "Simpson"), inline=T),
	           div(style = "display: none;",
	               checkboxInput("taxa_are_rows", label = "", value = T) )
	    ),
	    column(7,
	           radioButtons("plotTypeRadio", label="Visualization type:",
	                        choices = c("Boxplot" = "boxplot",
	                                    "Dot plot" = "dotplot"),
	                        selected = c("boxplot"), inline=T)
	    )
	  ),
	  fluidRow(
	    column(6,
	           uiOutput("category")
	    )
	  ),
	  fluidRow(
	    column(12,
	           div(
	             style = "position:relative",
	             plotOutput("abundanceChart",
	                        hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
	                        click = clickOpts("plot_click"), width = "100%"),
	             uiOutput("hover_info")
	           ) 
	    )
	  ),
	  fluidRow(
	    column(12,
	           dataTableOutput("sample_subset")
	    )
	  )
	    ) # end div id = "app-content",
	  ) # end hidden
	) # end fluidPage
) # end shinyUI