library(shiny)

shinyUI(
	fluidPage(
	  fluidRow(
	    column(5, 
	           checkboxGroupInput("measureCheckBox", label="Measure(s):",
	                              choices = c("Chao1" = "Chao1",
	                                          "ACE" = "ACE",
	                                          "Shannon" = "Shannon",
	                                          "Simpson" = "Simpson",
	                                          "Fisher" = "Fisher"),
	                              selected = c("Chao1", "Shannon"), inline=T),
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
	)
) # end shinyUI