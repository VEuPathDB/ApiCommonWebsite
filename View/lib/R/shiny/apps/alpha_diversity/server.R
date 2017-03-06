library(shiny)
library(ggplot2)
library(phyloseq)
library(reshape2)
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
   
    df_abundance$Abundance <- round(df_abundance$Abundance*100)
    
    df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "Abundance")
    OTU_MATRIX <- df_abundance.formatted[,8:ncol(df_abundance.formatted)]
    OTU = otu_table(OTU_MATRIX, taxa_are_rows = input$taxa_are_rows)
    
    TAX_MATRIX <- df_abundance.formatted[,1:7]
    TAX_MATRIX <- as.matrix(TAX_MATRIX)
    TAX <- tax_table(TAX_MATRIX)
    SAMPLE <- sample_data(df_sample.formatted)

    phyloseq(OTU, TAX, SAMPLE)
  })
	
	output$abundanceChart <- renderPlot({
	  physeqobj <- physeq()
	  if(!is.null(input$category)){
	    if(identical(input$category, "All Samples")){
	      chart <- plot_richness(physeqobj, measures = input$measureCheckBox)+theme(
	        panel.grid.major.x = element_blank()
	      )+geom_point(size = 4, alpha= 0.5)
	    }else{
	      chart <- plot_richness(physeqobj, x=hash_sample_names[[input$category]], measures = input$measureCheckBox)+theme(
	        panel.grid.major.x = element_blank()
	      )+geom_point(size = 4, alpha= 0.5)
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
	    if (is.null(hover$x))
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
	        "position:absolute; z-index:100; background-color: rgba(245, 245, 245, 0.85); "
	        ,
	        "left:",
	        left_px + 2,
	        "px; top:",
	        top_px + 2,
	        "px;"
	      )
	    near_points <- nearPoints(richness_default, hover)
	    physeqobj <- physeq()
	    if(identical(input$category, "All Samples")){
	      if(nrow(near_points) > 0){
	        lvls <- rownames(sample_data(physeqobj))
	        hover_sample <- lvls[round(hover$x)]
	        alpha_and_sample <- ""
	        for(i in 1:nrow(near_points)){
	          if(!is.na(near_points[i,"se"]) && near_points[i,"se"]!=0){
	            alpha_and_sample <- paste0(alpha_and_sample, "<b>Sample: </b>",near_points[i,"samples"],
	                                       "<br><b>Alpha diversity: </b>", sprintf("%.3f [SE: %.5f]",near_points[i,"value"],
	                                                                               near_points[i,"se"]),"<br>")
	          }else{
	            alpha_and_sample <- paste0(alpha_and_sample, "<b>Sample: </b>",near_points[i,"samples"],
	                                       "<br><b>Alpha diversity: </b>", sprintf("%.3f",near_points[i,"value"]),"<br>")
	          }
	        }
	        wellPanel(style = style,
	                  tags$b("Measure: "),
	                  hover$panelvar1,
	                  br(),
	                  HTML(alpha_and_sample)
	        )
	      }
	    }else{
	      if(nrow(near_points) > 0){
	        category_value <- sample_data(physeqobj)[,hash_sample_names[[input$category]]]
	        lvls <- levels(as.factor(category_value[[1]]))
	        hover_category <- lvls[round(hover$x)]
	        alpha_and_sample <- ""
	        for(i in 1:nrow(near_points)){
	          if(!is.na(near_points[i,"se"]) && near_points[i,"se"]!=0){
	            alpha_and_sample <- paste0(alpha_and_sample, "<b>Sample: </b>",near_points[i,"samples"],
	                                       "<br><b>Alpha diversity: </b>", sprintf("%.3f [SE: %.5f]",near_points[i,"value"],
	                                                                               near_points[i,"se"]),"<br>")
	          }else{
	            alpha_and_sample <- paste0(alpha_and_sample, "<b>Sample: </b>",near_points[i,"samples"],
	                                       "<br><b>Alpha diversity: </b>", sprintf("%.3f",near_points[i,"value"]),"<br>")
	          }
	        }
	        wellPanel(style = style,
	                  tags$b("Measure: "),
	                  hover$panelvar1,
	                  br(),
	                  tags$b(paste(input$category, ": ")),
	                  hover_category,
	                  br(),
	                  HTML(alpha_and_sample)
	        )
	      }
	    }
	})
})
