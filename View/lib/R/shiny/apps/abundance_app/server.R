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
  hash_count_samples <- NULL
  richness_default <- NULL
  plot_build <- NULL
  physeq <- reactive({
    #Change with the file with abundances
    df_abundance <-
      read.csv(
        getWdkDatasetFile('TaxaRelativeAbundance.tab', session, FALSE, dataStorageDir),
        sep = "\t",
        col.names = c("Sample","Taxon", "Kingdom", "Phylum", "Class", "Order", "Family", "Genus", "Species", "RelativeAbundance", "AbsoluteAbundance", "Nada")
      )
    
    # Change with the Characteristics file
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
    
    df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
    OTU_MATRIX <- df_abundance.formatted[,8:ncol(df_abundance.formatted)]
    OTU = otu_table(OTU_MATRIX, taxa_are_rows = input$taxa_are_rows)
    
    TAX_MATRIX <- df_abundance.formatted[,1:7]
    TAX_MATRIX <- as.matrix(TAX_MATRIX)
    TAX <- tax_table(TAX_MATRIX)
    SAMPLE <- sample_data(df_sample.formatted)
    merged_phyloseq <- tax_glom(phyloseq(OTU, TAX, SAMPLE), input$taxonLevel)
    
    categories <- columns
    new_columns <- 0
    k <- 1
    for(i in 1:length(columns)){
      unique_factors <- as.factor(sample_data(merged_phyloseq)[[hash_sample_names[[columns[i]]]]]) 
      if(length(levels(unique_factors)) > 1){
        new_columns[k] <- paste0(columns[i], " (",length(levels(unique_factors)), ")")
        hash_count_samples[[new_columns[k]]] <<- columns[i]
        k <- k+1
      }
    }
    columns <<- new_columns
    
    merged_phyloseq
  })
  
  output$abundanceChart <- renderPlot({
    physeqobj <- physeq()
    if(!is.null(input$category)){
      if(identical(input$category, "All Samples")){
        chart <- plot_bar(physeqobj, fill=input$taxonLevel)+theme(legend.position="none")+coord_flip()
        
      }else{
				chart <- plot_bar(physeqobj, fill=input$taxonLevel)+facet_grid(as.formula(paste(hash_sample_names[[hash_count_samples[[input$category]]]], "~ .")), scale='free_y', space="free_y")+theme(legend.position="none")+coord_flip()
      }
      richness_default <<- chart
      plot_build <<- ggplot_build(chart)
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
    
    
    if(identical(input$category, "All Samples")){
    	hover_sample <- lvls[round(hover$y)]
    	hover_data<-subset(richness_default$data, Sample==hover_sample & Abundance>0)
    	
    	unique_y<-unique(subset(layer_data(richness_default), x==round(hover$y))$y)
    	unique_y<-unique_y[unique_y>0]
    	abundances_filtered <- get_abundances_from_plot(unique_y)
    	
    	abundances_joined <- join_abundance(abundances_filtered, hover_data)
    	all_sum <- cumsum(abundances_joined$Abundance)
    	index_abundance_hover = get_abundance_index(all_sum, hover$x)
    	
    	if(index_abundance_hover == -1)
    		      return(NULL)

    	wellPanel(style = style,
			              tags$b("Sample: "),
			      				hover_sample,
			      				br(),
			      				tags$b(paste(input$taxonLevel, ": ")),
			      				abundances_joined[index_abundance_hover,input$taxonLevel],
			      				br(),
			      				tags$b("Abundance: "),
			      				abundances_joined[index_abundance_hover,"Abundance"]
			    			  )
    }else{
    	pnl_layout <- plot_build$layout$panel_layout
    	panel_index <- pnl_layout[ pnl_layout[[hash_sample_names[[hash_count_samples[[input$category]]]]]] == hover$panelvar1 , ]$PANEL
    	lvls <- plot_build$layout$panel_ranges[[panel_index]]$y.labels
    	hover_sample <- lvls[round(hover$y)]
    	if(!is.na(hover_sample)){
    		hover_data<-subset(richness_default$data, Sample==hover_sample & Abundance>0)
    		unique_y<-unique(subset(layer_data(richness_default), x==round(hover$y) & PANEL==panel_index)$y)
    		unique_y<-unique_y[unique_y>0]
    		abundances_filtered <- get_abundances_from_plot(unique_y)
    		abundances_joined <- join_abundance(abundances_filtered, hover_data)
    		all_sum <- cumsum(abundances_joined$Abundance)
    		
    		index_abundance_hover = get_abundance_index(all_sum, hover$x)
    		
    		if(index_abundance_hover == -1)
    			return(NULL)
    		
	      wellPanel(style = style,
          tags$b("Sample: "),
          hover_sample,
          br(),
          tags$b("Category: "),
          hover$panelvar1,
          br(),
          tags$b(paste(input$taxonLevel, ": ")),
          abundances_joined[index_abundance_hover,input$taxonLevel],
          br(),
          tags$b("Abundance: "),
          abundances_joined[index_abundance_hover,"Abundance"]
	      )
    	}else{
    		return(NULL)
    	}	
    }
  })
  
  observeEvent(input$plot_click, {
    click <- input$plot_click
    if (is.null(click$y))
      return(NULL)
    lvls <- rownames(sample_data(physeq()))
    df <- richness_default$data
    
    if(identical(input$category, "All Samples")){
    	sample <- lvls[round(click$y)]
      df <- df[,c("Sample", input$taxonLevel, "Abundance")]
      selected_sample = subset(df, Sample == sample & Abundance > 0)
      output$sample_subset <- renderDataTable(selected_sample,options = list(aaSorting = list(list(2, 'desc'),list(1, 'asc'))) )
    }else{
    	pnl_layout <- plot_build$layout$panel_layout
    	panel_index <- pnl_layout[ pnl_layout[[hash_sample_names[[hash_count_samples[[input$category]]]]]] == click$panelvar1 , ]$PANEL
    	lvls <- plot_build$layout$panel_ranges[[panel_index]]$y.labels
    	
    	sample <- lvls[round(click$y)]
    	selected_sample = subset(df, Sample == sample & Abundance > 0)
      
      if(!identical(selected_sample[1, hash_sample_names[[hash_count_samples[[input$category]]]]], click$panelvar1))
        return(NULL)
      selected_sample <- selected_sample[,c("Sample", input$taxonLevel, "Abundance")]
      output$sample_subset <- renderDataTable(selected_sample,options = list(aaSorting = list(list(2, 'desc'),list(1, 'asc'))) )
    }
  })
})
