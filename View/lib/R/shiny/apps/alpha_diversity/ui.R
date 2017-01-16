library(shiny)

shinyUI(
	fluidPage(
    checkboxGroupInput("measureCheckBox", label="Measure(s):",
                     choices = c("Chao1" = "Chao1",
                                 "ACE" = "ACE",
                       "Shannon" = "Shannon",
                       "Simpson" = "Simpson",
                       "Fisher" = "Fisher"),
                     selected = c("Chao1", "Shannon"), inline=T),
    div(style = "display: none;",
	     checkboxInput("taxa_are_rows", label = "", value = TRUE)
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