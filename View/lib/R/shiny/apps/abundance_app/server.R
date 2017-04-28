# Declaring the packages
library(shiny)
library(ggplot2)
library(scales)
library(phyloseq)
library(data.table)
source("functions.R")
source("config.R")
source("../../lib/wdkDataset.R")

shinyServer(function(input, output, session) {
  # Declaring some global variables
  # df_abundance, df_sample and df_sample.formatted are declared global to avoid 
  # multiple file reading in the reactive section
  df_abundance <- NULL
  df_sample <- NULL
  df_sample.formatted <- NULL
  
  # global objects to read in more than one function
  columns <- NULL
  hash_sample_names<- NULL
  hash_count_samples <- NULL
  ggplot_object <- NULL
  ggplot_build_object <- NULL
  
  abundance_otu <- NULL
  abundance_taxa <- NULL
  
  highest_otu_tax_factor<-NULL
  
  # variables to define some plot parameters
  number_of_taxa <- 10
  maximum_samples_without_resizing <- 65
  minimum_height_after_resizing <- 6
  
  physeq <- reactive({
    # Change with the file with abundances
    if(is.null(df_abundance)){
      start.time <- Sys.time()
      df_abundance <<-
      read.csv(
        getWdkDatasetFile('TaxaRelativeAbundance.tab', session, FALSE, dataStorageDir),
          sep = "\t",
          col.names = c("Sample","Taxon", "Kingdom", "Phylum", "Class", "Order", "Family", "Genus", "Species", "RelativeAbundance", "AbsoluteAbundance", "EmptyColumn"),
          colClasses = c("character", "integer", "character", "character", "character", "character", "character", "character", "character", "numeric", "integer", "character")
        )
      
      # Change with the Characteristics file
      df_sample <<-
      read.csv(
        getWdkDatasetFile('Characteristics.tab', session, FALSE, dataStorageDir),
          sep = "\t",
          col.names = c("SampleName", "Source", "Property", "Value", "Type", "Filter", "EmptyColumn"),
          colClasses = c("character", "character", "character", "character", "character", "character", "character")
        ) 
      
      df_sample.formatted <<- dcast(data = df_sample,formula = SampleName~Property, value.var = "Value")
      
      end.time <- Sys.time()
      time.taken <- end.time - start.time
      write(paste("Elapsed time to load the files: ", time.taken), stderr())
    }
    
    rownames(df_sample.formatted) <- df_sample.formatted[,1]
    columns <<- colnames(df_sample.formatted)
    corrected_columns <-  make.names(columns)
    colnames(df_sample.formatted) <- corrected_columns
    names(corrected_columns) <- columns 
    hash_sample_names <<- corrected_columns
    
    start.time <- Sys.time()
    if(identical(input$taxonLevel, "Phylum")){
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 3
      column_tax <- 2
    }else if(identical(input$taxonLevel, "Class")){
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 4
      column_tax <- 3
    }else if(identical(input$taxonLevel, "Order")){
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 5
      column_tax <- 4
    }else if(identical(input$taxonLevel, "Family")){
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 6
      column_tax <- 5
    }else if(identical(input$taxonLevel, "Genus")){
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 7
      column_tax <- 6
    }else{
      df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")
      column_otu <- 8
      column_tax <- 7
    }
    end.time <- Sys.time()
    time.taken <- end.time - start.time
    write(paste("Elapsed time to dcast the abundance: ", time.taken), stderr())
    
    start.time <- Sys.time()
    
    output$sample_subset <- renderDataTable(NULL)
    abundance_otu <<- df_abundance.formatted[,column_otu:ncol(df_abundance.formatted)]
    
    filtered_abundance_otu <- filter_n_abundant(number_of_taxa, abundance_otu)
    
    rows_to_maintain <- rowSums(filtered_abundance_otu)>0
    
    filtered_abundance_otu <- filtered_abundance_otu[rows_to_maintain,]
    
    OTU = otu_table(filtered_abundance_otu, taxa_are_rows = input$taxa_are_rows)
    
    highest_otu <- get_n_abundant_overall(number_of_taxa, abundance_otu)
    
    abundance_taxa <<- df_abundance.formatted[,1:column_tax]
    abundance_taxa <<- fix_taxonomy_names(abundance_taxa)
    
    abundance_taxa_for_phyloseq<-format_abundant_taxa(number_of_taxa, abundance_taxa)
    
    abundance_taxa_for_phyloseq <- abundance_taxa_for_phyloseq[rows_to_maintain,]
    
    TAX <- tax_table(as.matrix(abundance_taxa_for_phyloseq))
    
    highest_otu_tax_factor<<-abundance_taxa_for_phyloseq[rownames(highest_otu),column_tax]
    
    SAMPLE <- sample_data(df_sample.formatted)
    merged_phyloseq <- phyloseq(OTU, TAX, SAMPLE)
    
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
    end.time <- Sys.time()
    time.taken <- end.time - start.time
    write(paste("Elapsed time to create phyloseq object: ", time.taken), stderr())
    merged_phyloseq
  })
  
  output$abundanceChart <- renderUI({
    physeqobj <- physeq()
    if(!is.null(input$category)){
      quantity_samples <- length(sample_names(physeqobj))
      if(quantity_samples <= maximum_samples_without_resizing){
        plotOutput("plotWrapper",hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
                   click = clickOpts("plot_click"), width = "100%")
      }else{
        plotOutput("plotWrapper",hover = hoverOpts("plot_hover", delay = 100, delayType = "debounce"),
                   click = clickOpts("plot_click"), width = "100%", height = quantity_samples*minimum_height_after_resizing)
      }
      
    }
    
  })
  
  output$plotWrapper <- renderPlot({
    physeqobj <- physeq()
    start.time <- Sys.time()
    if(!is.null(input$category)){
      cbPalette <- c("#FFFFCC", "#FF00CC", "#FF99CC", "#FFCC99", "#FFFF00", "#FF9900", "#FF3300", "#CCFFFF", "#CC99FF", "#CC00FF", "#CCFF66", "#CC6666", "#CC0066", "#99FFCC", "#9999CC", "#9900CC","#99FF33", "#999933", "#996633", "#990033", "#66FF00", "#669900", "#660000", "#33FFFF", "#3399FF", "#3300FF", "#33FF00", "#339900", "#330000", "#0000CC", "#000066", "#003333")
      cbPalette <- rep(cbPalette, 3)
      if(identical(input$category, "All Samples")){
        chart <-
          plot_bar(physeqobj, fill=input$taxonLevel)+
          theme(
                axis.title = element_text(family = "Trebuchet MS", color="#666666", face="bold", size=16),
                axis.text.y=element_blank(),
                axis.ticks.y = element_blank()
                # legend.position = "none"
                )+
          scale_fill_manual(values=cbPalette, breaks=highest_otu_tax_factor, name="Top Overall Abundances")+
          # scale_fill_discrete(name="Top Overall Abundances", breaks=highest_otu_tax_factor)+
          labs(x="Samples", y="OTU Abundance")+
          coord_flip(expand=F)
      }else{
				chart <-
				  plot_bar(physeqobj, fill=input$taxonLevel)+
				  facet_grid(as.formula(paste(hash_sample_names[[hash_count_samples[[input$category]]]], "~ .")), scale='free_y', space="free_y")+
				  theme(
				        axis.title = element_text(family = "Trebuchet MS", color="#666666", face="bold", size=16),
				        axis.text.y=element_blank(),
				        axis.ticks.y = element_blank()
				        )+
				  scale_fill_manual(values=cbPalette, breaks=highest_otu_tax_factor, name="Top Overall Abundances")+
				  # scale_fill_discrete(name="Top Overall Abundances", breaks=highest_otu_tax_factor)+
				  labs(x="Samples", y="OTU Abundance")+
				  coord_flip(expand=F)
      }
      ggplot_object <<- chart
      ggplot_build_object <<- ggplot_build(chart)
    }else{
      chart <- NULL
    }
    end.time <- Sys.time()
    time.taken <- end.time - start.time
    write(paste("Elapsed time to build the plot: ", time.taken), stderr())
    chart
  })
  
  output$category <- renderUI({
    lvls <- columns
    lvls[1] <- "All Samples"
    selectInput("category", label = "Split by the category: ", 
                choices = lvls, selected = 1, width = '100%')
    
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
    	hover_data<-subset(ggplot_object$data, Sample==hover_sample & Abundance>0)
    	
    	unique_y<-unique(subset(layer_data(ggplot_object), x==round(hover$y))$y)
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
    	pnl_layout <- ggplot_build_object$layout$panel_layout
    	panel_index <- pnl_layout[ pnl_layout[[hash_sample_names[[hash_count_samples[[input$category]]]]]] == hover$panelvar1 , ]$PANEL
    	
    	if(length(panel_index) > 0){
    	  lvls <- ggplot_build_object$layout$panel_ranges[[panel_index]]$y.labels
    	  hover_sample <- lvls[round(hover$y)]
    	  if(!is.na(hover_sample)){
    	    hover_data<-subset(ggplot_object$data, Sample==hover_sample & Abundance>0)
    	    unique_y<-unique(subset(layer_data(ggplot_object), x==round(hover$y) & PANEL==panel_index)$y)
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
    df <- ggplot_object$data
    
    if(identical(input$category, "All Samples")){
    	sample <- lvls[round(click$y)]
    }else{
    	pnl_layout <- ggplot_build_object$layout$panel_layout
    	panel_index <- pnl_layout[ pnl_layout[[hash_sample_names[[hash_count_samples[[input$category]]]]]] == click$panelvar1 , ]$PANEL
    	lvls <- ggplot_build_object$layout$panel_ranges[[panel_index]]$y.labels
    	
    	sample <- lvls[round(click$y)]
    }
    
    raw_data<-data.frame(rep(sample, nrow(abundance_taxa)), abundance_taxa[,input$taxonLevel],"Abundance"=abundance_otu[,sample])
    raw_data<-subset(raw_data, Abundance>0)
    raw_data$Abundance<-format(raw_data$Abundance, scientific = F)
    colnames(raw_data)<-c("Sample","Species", "Relative Abundance")
    output$sample_subset <- renderDataTable(raw_data, 
                                            options = list(
                                              order = list(list(2, 'desc'),list(1, 'asc'))
                                              )
                                            )
  })
})
