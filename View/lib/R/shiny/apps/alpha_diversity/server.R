library(shiny)
library(ggplot2)
library(phyloseq)
library(data.table)
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
  ggplot_data <- NULL
  ggplot_build_object <- NULL
  
  abundance_otu <- NULL
  abundance_taxa <- NULL
  
  physeq <- reactive({
    
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
    
    rownames(df_sample.formatted) <<- df_sample.formatted[,1]
    columns <<- colnames(df_sample.formatted)
    corrected_columns <-  make.names(columns)
    colnames(df_sample.formatted) <<- corrected_columns
    names(corrected_columns) <- columns
    
    hash_sample_names <<- corrected_columns
    
    df_abundance.formatted <- dcast(data = df_abundance,formula = Kingdom+Phylum+Class+Order+Family+Genus+Species~Sample,fun.aggregate = sum,value.var = "AbsoluteAbundance")
    if(ncol(df_abundance.formatted) == 8){
      OTU_MATRIX <- df_abundance.formatted[,8, drop=F]
    }else{
      OTU_MATRIX <- df_abundance.formatted[,8:ncol(df_abundance.formatted)]
    }
    
    OTU = otu_table(OTU_MATRIX, taxa_are_rows = input$taxa_are_rows)
    
    TAX_MATRIX <- df_abundance.formatted[,1:7]
    TAX_MATRIX <- as.matrix(TAX_MATRIX)
    TAX <- tax_table(TAX_MATRIX)
    SAMPLE <- sample_data(df_sample.formatted)
    
    categories <- columns
    new_columns <- 0
    k <- 1
    merged_phyloseq<-phyloseq(OTU, TAX, SAMPLE)
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
        if(identical(input$plotTypeRadio, "dotplot")){
          chart <- plot_richness(physeqobj, measures = input$measureCheckBox)+theme(
            panel.grid.major.x = element_blank()
          )+geom_point(size = 4, alpha= 0.5)+coord_fixed()
        }else{
          rich <- estimate_richness(physeqobj, measures = input$measureCheckBox)
          rich$SampleName <- gsub("\\.", "\\-", rownames(rich))
          
          data_melted<-melt(rich, id.vars = c("SampleName"),  measure.vars=input$measureCheckBox)
          abundance_otu <<- data_melted
          chart<-ggplot(data_melted, aes(variable, value))+geom_boxplot()+
            facet_wrap(~ variable, scales="free")+
            theme(
              axis.text.x=element_blank(),
              axis.ticks.x = element_blank()
            )+
            labs(x="All Samples", y="Alpha Diversity Measure")
          
        }
      }else{
        if(identical(input$plotTypeRadio, "dotplot")){
          chart <- plot_richness(physeqobj, x=hash_sample_names[[hash_count_samples[[input$category]]]], measures = input$measureCheckBox)+
            theme(panel.grid.major.x = element_blank(), strip.text.y = element_text(size=18))+
            geom_point(size = 4, alpha= 0.5) 
        }else{
          category<-hash_sample_names[[hash_count_samples[[input$category]]]]
          
          rich <- estimate_richness(physeqobj, measures = input$measureCheckBox)
          rich$SampleName <- gsub("\\.", "\\-", rownames(rich))
          
          df_sample_selected <- df_sample.formatted[,c("SampleName", category)]
          richness_merged <- merge(df_sample_selected, rich, by.x = "SampleName", by.y = "SampleName")
          
          data_melted<-melt(richness_merged, id.vars = c("SampleName", category),  measure.vars=input$measureCheckBox)
          
          chart<-ggplot(data_melted, aes_string(category, "value"))+geom_boxplot()+
            facet_wrap(as.formula("~ variable "), scales = "free_y")+
            labs(x=stringi::stri_trans_totitle(hash_count_samples[[input$category]]), y="Alpha Diversity Measure")
        }
      }
      ggplot_data <<- chart$data
      ggplot_build_object <<- ggplot_build(chart)
    }else{
      chart <- NULL
    }
    output$sample_subset <- renderDataTable(NULL)
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
    physeqobj <- physeq()
    lvls <- rownames(sample_data(physeqobj))
    
    if (is.null(hover$x) || round(hover$x) <0 || round(hover$y)<0 || is.null(hover$y))
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
    
    if(identical(input$plotTypeRadio, "dotplot") ){
      near_points <- nearPoints(ggplot_data, hover)

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
          category_value <- sample_data(physeqobj)[,hash_sample_names[[hash_count_samples[[input$category]]]]]
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
                    tags$b(paste(hash_count_samples[[input$category]], ": ")),
                    hover_category,
                    br(),
                    HTML(alpha_and_sample)
          )
        }
      }
    }else{ # end if(identical(input$plotTypeRadio, "dotplot") ){
      gg_data <- ggplot_build_object$data[[1]]
      subBoxplot<-subset(gg_data,hover$x>=xmin & hover$x<=xmax & hover$y>=ymin & hover$y<=ymax & PANEL==match(hover$panelvar1, input$measureCheckBox))
      alpha_and_sample <- NULL
      if(identical(input$category, "All Samples")){
        if(nrow(subBoxplot)>0){
          alpha_and_sample <- ""
          for(i in 1:nrow(subBoxplot)){
            alpha_and_sample <- paste0(alpha_and_sample,
                                       sprintf("<b>Alpha diversity values:</b><br>&nbsp;&nbsp;<b>min: </b>%f - <b>max: </b>%f<br>
                                               &nbsp;&nbsp;<b>25th percentile: </b>%f<br>&nbsp;&nbsp;<b>Median: </b>%f<br>&nbsp;&nbsp;<b>75th percentile: </b>%f<br>",
                                               subBoxplot[i,"ymin"],subBoxplot[i,"ymax"],subBoxplot[i,"lower"],subBoxplot[i,"middle"],subBoxplot[i,"upper"]) )
          }
          
        }else{
          near_points <- nearPoints(ggplot_data, hover)
          if(nrow(near_points) > 0){
            alpha_and_sample <- ""
            for(i in 1:nrow(near_points)){
              alpha_and_sample <- paste0(alpha_and_sample,
                                         sprintf("<b>Sample: </b>%s<br><b>Alpha diversity: </b>%.3f<br>",
                                                 near_points[i,"SampleName"],near_points[i,"value"]))
            }
          }
        }
        # return(NULL)
      }else{ # if(identical(input$category, "All Samples")){
        column_category<-hash_sample_names[[hash_count_samples[[input$category]]]]
        original_category<-hash_count_samples[[input$category]]
        all_groups <- levels(as.factor(ggplot_data[,column_category]))
        if(nrow(subBoxplot)>0){
          alpha_and_sample<-""
          for(i in 1:nrow(subBoxplot)){
            alpha_and_sample <- paste0(alpha_and_sample,
                                     sprintf("<b>Alpha diversity values:</b><br>&nbsp;&nbsp;<b>min: </b>%f - <b>max: </b>%f<br>
                                             &nbsp;&nbsp;<b>25th percentile: </b>%f<br>&nbsp;&nbsp;<b>Median: </b>%f<br>&nbsp;&nbsp;<b>75th percentile: </b>%f<br><b>%s: </b>%s<br>",
                                             subBoxplot[i,"ymin"],subBoxplot[i,"ymax"],subBoxplot[i,"lower"],subBoxplot[i,"middle"], subBoxplot[i,"upper"], original_category, all_groups[subBoxplot[i,"group"]]) )
          }
        }else{
          near_points <- nearPoints(ggplot_data, hover)
          if(nrow(near_points) > 0){
            alpha_and_sample <- ""
            for(i in 1:nrow(near_points)){
              alpha_and_sample <- paste0(alpha_and_sample,
                                         sprintf("<b>Sample: </b>%s<br><b>Alpha diversity: </b>%.3f<br><b>%s</b>: %s<br>",
                                                 near_points[i,"SampleName"],near_points[i,"value"], original_category, near_points[i,column_category]))
            }
          }
        }
      }# else
      if(!is.null(alpha_and_sample)){
        wellPanel(style = style,
                  tags$b("Measure: "),
                  hover$panelvar1,
                  br(),
                  HTML(alpha_and_sample)
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
    
    if(identical(input$category, "All Samples")){
      output_table <- subset(ggplot_data, ggplot_data$variable==click$panelvar1, select=c("SampleName", "variable", "value"))
      colnames(output_table)<-c("Sample", "Measure", "Alpha Diversity")
    }else{
      column_category<-hash_sample_names[[hash_count_samples[[input$category]]]]
      original_category<-hash_count_samples[[input$category]]
      
      output_table <- subset(ggplot_data, ggplot_data$variable==click$panelvar1, select=c("SampleName", "variable", "value", column_category))
      colnames(output_table)<-c("Sample", "Measure", "Alpha Diversity", original_category)
    }
    
    output$sample_subset <- renderDataTable(output_table, 
                                            options = list(
                                              order = list(list(2, 'desc'))
                                            )
    )
  })
  shinyjs::hide(id = "loading-content", anim = TRUE, animType = "fade")
  shinyjs::show("app-content")
}) # end shinyServer
