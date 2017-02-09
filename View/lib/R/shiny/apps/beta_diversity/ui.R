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
	                "Mountford"="mountford",
	                "Gower"="gower",
	                "Raup"="raup",
	                "Binomial"="binomial",
	                "w"="w",
	                "-1"="-1",
	                "c"="c",
	                "wb"="wb",
	                "r"="r",
	                "e"="e",
	                "t"="t",
	                "me"="me",
	                "j"="j",
	                "m"="m",
	                "-2"="-2",
	                "co"="co",
	                "cc"="cc",
	                "-3"="-3",
	                "l"="l",
	                "19"="19",
	                "hk"="hk",
	                "rlb"="rlb",
	                "sim"="sim",
	                "gl"="gl",
	                "z"="z"
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