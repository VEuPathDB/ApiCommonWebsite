library(shiny)
library(ggplot2)
library(phyloseq)
library(data.table)
source("config.R")
source("../../lib/wdkDataset.R")

shinyServer(function(input, output, session) {

  df_abundance <- NULL
  df_sample <- NULL

  columns <- NULL
  hash_sample_names<- NULL
  hash_count_samples <- NULL
  richness_default <- NULL
  plot_build <- NULL

  physeq <- reactive({
    #Change with the file with abundances
    if(is.null(df_abundance)){
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
    }

    df_sample.formatted <- dcast(data = df_sample,formula = SampleName~Property, value.var = "Value")
    
    rownames(df_sample.formatted) <- df_sample.formatted[,1]
    columns <<- colnames(df_sample.formatted)
    corrected_columns <-  make.names(columns)
    colnames(df_sample.formatted) <- corrected_columns
    names(corrected_columns) <- columns
    hash_sample_names <<- corrected_columns
    
    SAMPLE <- sample_data(df_sample.formatted)
    
    new_columns <- 0
    k <- 1
    for(i in 1:length(columns)){
      unique_factors <- as.factor(df_sample.formatted[[hash_sample_names[[columns[i]]]]])
      if(length(levels(unique_factors)) > 1){
        new_columns[k] <- paste0(columns[i], " (",length(levels(unique_factors)), ")")
        hash_count_samples[[new_columns[k]]] <<- columns[i]
        k <- k+1
      }
    }
    columns <<- new_columns
    updateSelectizeInput(session, "category",
                         choices = c("All Samples", columns[2:length(columns)]),
                         selected = "All Samples",
                         options = list(placeholder = 'Type the category to split'),
                         server = TRUE)
    
    df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "RelativeAbundance")

    OTU_MATRIX <- df_abundance.formatted[,8:ncol(df_abundance.formatted)]
    OTU = otu_table(OTU_MATRIX, taxa_are_rows = input$taxa_are_rows)

    TAX_MATRIX <- df_abundance.formatted[,1:7]
    TAX_MATRIX <- as.matrix(TAX_MATRIX)
    TAX <- tax_table(TAX_MATRIX)

    merged_phyloseq <- phyloseq(OTU, TAX, SAMPLE)

    merged_phyloseq
  })

	output$abundanceChart <- renderPlot({
	  physeqobj = physeq()

	  if(!identical(input$category, "")){
      ordination = ordinate(physeqobj, method = "PCoA", distance = input$distance)
      if(identical(input$category, "All Samples")){
        chart <- plot_ordination(physeqobj, ordination)+theme(
          panel.grid.major.x = element_blank(), legend.position="none")+geom_point(size = 4, alpha= 0.5)
      }else{
        chart <- plot_ordination(physeqobj, ordination, color=hash_sample_names[[hash_count_samples[[input$category]]]])+theme(
          panel.grid.major.x = element_blank())+labs(color=hash_count_samples[[input$category]])+geom_point(size = 4, alpha= 0.5)
      }
      richness_default <<- chart$data
	  }else{
	    chart<-NULL
	  }
	  chart
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
    if(nrow(near_points) > 0){
      if(identical(input$category, "All Samples")){
        category_hover = "SampleName"
        text_hover <- ""
        for(i in 1:nrow(near_points)){
          text_hover <- paste0(text_hover,
                               "<b>Sample: </b>",near_points[i,"SampleName"],
                               "<br><b>Axis.1: </b>", sprintf("%.3f",near_points[i,"Axis.1"]),
                               "<br><b>Axis.2: </b>", sprintf("%.3f<br>",near_points[i,"Axis.2"]) )
        }
        wellPanel(style = style,
                  HTML(text_hover)
        )
      }else{
        category_hover = hash_sample_names[[hash_count_samples[[input$category]]]]
        text_hover <- ""
        for(i in 1:nrow(near_points)){
          text_hover <- paste0(text_hover,
                               "<b>Sample: </b>",near_points[i,"SampleName"],
                               sprintf("<br><b>%s: </b>%s",hash_count_samples[[input$category]], near_points[i,category_hover]),
                               "<br><b>Axis.1: </b>", sprintf("%.3f",near_points[i,"Axis.1"]),
                               "<br><b>Axis.2: </b>", sprintf("%.3f<br>",near_points[i,"Axis.2"]) )
        }
        wellPanel(style = style,
                  HTML(text_hover)
        )
      }
    }
	})
	shinyjs::hide(id = "loading-content", anim = TRUE, animType = "fade")
	shinyjs::show("app-content")
})
