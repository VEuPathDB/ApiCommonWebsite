library(shiny)
library(ggplot2)
library(phyloseq)
library(reshape2)
source("functions.R")
source("config.R")
source("../../lib/wdkDataset.R")

shinyServer(function(input, output, session) {
  columns <- NULL
  hash_sample_names<- NULL
  
  physeq <- reactive({
    #Change with the file with abundances
    df_abundance <-
      read.csv(
        getWdkDatasetFile('TaxaRelativeAbundance.tab', session, FALSE, dataStorageDir),
        sep = "\t",
        col.names = c("Sample","Taxon", "Kingdom", "Phylum", "Class", "Order", "Family", "Genus", "Species", "Abundance", "EmptyColumn")
      )
    
    # Change with the Metadata file
    df_sample <-
      read.csv(
        getWdkDatasetFile('Characteristics.tab', session, FALSE, dataStorageDir),
        sep = "\t",
        col.names = c("SampleName", "Source", "Property", "Value", "Type", "Filter", "EmptyColumn")
      )
    
    df_sample.formatted <- dcast(data = df_sample,formula = SampleName~Property, value.var = "Value")
    rownames(df_sample.formatted) <- df_sample.formatted[,1]
    columns <<- colnames(df_sample.formatted)
    corrected_columns <-  make.names(columns)
    colnames(df_sample.formatted) <- corrected_columns
    names(corrected_columns) <- columns
    
    hash_sample_names <<- corrected_columns
    
    df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "Abundance")
    OTU_MATRIX <- df_abundance.formatted[,8:ncol(df_abundance.formatted)]
    OTU = otu_table(OTU_MATRIX, taxa_are_rows = input$taxa_are_rows)
    
    TAX_MATRIX <- df_abundance.formatted[,1:7]
    TAX_MATRIX <- as.matrix(TAX_MATRIX)
    TAX <- tax_table(TAX_MATRIX)
    SAMPLE <- sample_data(df_sample.formatted)
    merged_phyloseq <- tax_glom(phyloseq(OTU, TAX, SAMPLE), input$taxonLevel)
    merged_phyloseq
  })
	
	output$abundanceChart <- renderPlot({
	  physeqobj <- physeq()
	  if(!is.null(input$category)){
		  if(identical(input$category, "All Samples")){
		  	chart <- plot_bar(physeqobj, fill=input$taxonLevel)+theme(legend.position="none")+coord_flip()
		  	
		  }else{
		  	chart <- plot_bar(physeqobj, fill=input$taxonLevel, facet_grid=as.formula(paste("~", hash_sample_names[[input$category]])))+theme(legend.position="none")+coord_flip()
		  }
	  	richness_default <<- chart$data
	  }else{
	  	chart <- NULL
	  }
    chart
	})
	
	output$category <- renderUI({
		lvls <- columns
		lvls[1] <- "All Samples"
		selectInput("category", label = "Category: ", 
								choices = lvls, selected = 1)
		
	})
	
	output$hover_info <- renderUI({
	    hover <- input$plot_hover
	    lvls <- rownames(sample_data(physeq()))
	    
	    if (is.null(hover$x) || round(hover$x) <0 || round(hover$y)<1 || round(hover$y) > length(lvls))
	      return(NULL)
	    
	    left_pct <-
	      (hover$x - hover$domain$left) / (hover$domain$right - hover$domain$left)
	    top_pct <-
	      (hover$domain$top - hover$y) / (hover$domain$top - hover$domain$bottom)
	    
	    left_px <-
	      hover$range$left + left_pct * (hover$range$right - hover$range$left)
	    top_px <-
	      hover$range$top + top_pct * (hover$range$bottom - hover$range$top)
	    
	    style <-
	      paste0(
	        "position:absolute; z-index:100; background-color: rgba(245, 245, 245, 0.85); ",
	        "left:",
	        left_px + 2,
	        "px; top:",
	        top_px + 2,
	        "px;"
	      )
	    hover_sample <- lvls[round(hover$y)]
	    
	    hover_data <- subset(richness_default, Sample==hover_sample & Abundance > 0)
	    all_sum <- cumsum(hover_data$Abundance)
	    index_abundance_hover = get_abundance_index(all_sum, hover$x)
	    if(index_abundance_hover == -1)
	      return(NULL)
	    
	    if(identical(input$category, "All Samples")){
  	    wellPanel(style = style,
          tags$b("Sample: "),
  				hover_sample,
  				br(),
  				tags$b(paste(input$taxonLevel, ": ")),
  				hover_data[index_abundance_hover,input$taxonLevel],
  				br(),
  				tags$b("Abundance: "),
  				hover_data[index_abundance_hover,"Abundance"]
			  )
	    }else{
	      # print(hover$panelvar1)
	      if(!identical(hover_data[1, hash_sample_names[[input$category]]], hover$panelvar1))
	        return(NULL)
	      wellPanel(style = style,
          tags$b("Sample: "),
          hover_sample,
          br(),
          tags$b("Category: "),
          hover$panelvar1,
          br(),
          tags$b(paste(input$taxonLevel, ": ")),
          hover_data[index_abundance_hover,input$taxonLevel],
          br(),
          tags$b("Abundance: "),
          hover_data[index_abundance_hover,"Abundance"]
	      )
	    }
	})
})