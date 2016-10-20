library(shiny)
library(ggplot2)
source("functions.R")
source("../../lib/wdkDataset.R")

shinyServer(function(input, output) {

  # any time the user change the taxonomic level the data is reactively reloaded
  data <- reactive({

    dat <- getWdkDataset(session, fetchStyle, 'TaxaRelativeAbundance.tab', FALSE, dataStorageDir)

    # Revert to hard-coded data file if necessary
    # dat <-
    #  read.csv(
    #    "MicrobiomeSampleByMetadata_TaxaRelativeAbundance.txt",
    #    sep = "\t",
    #    check.names = FALSE
    # )

    taxa.level <- input$taxa.level
    cols <-  c(3:taxa.level)
    dat$taxonomy <-
      apply(dat[, cols, drop = FALSE] , 1 , format.taxonomy)
    dat.show <- aggregate(x = dat[, 10], by = dat[, c(1, 12)], sum)
    colnames(dat.show) <- c("Sample", "Taxonomy", "Abundance")
    output$sample_subset <- renderDataTable(NULL)
    dat.show
  })

  output$abundanceChart <- renderPlot({
    ggplot(data = data(), aes(x = Sample, y = Abundance, fill = Taxonomy)) +
      geom_bar(stat = "identity") +
      theme(
        legend.position = "none",
        panel.grid.major.x = element_blank(),
        axis.text.x = element_text(angle = 45, hjust = 1)
      )
  })
  # this will show the hover chart popup
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
    df <- data()
    lvls <- levels(df$Sample)
    sample <- lvls[round(hover$x)]
    selected_sample = subset(df, df$Sample == sample)
    abundance_sample <- selected_sample$Abundance
    taxonomy_sample <- selected_sample$Taxonomy
    total_abundance <- 0
    index_taxonomy <- -1
    for (i in seq_along(abundance_sample)) {
      total_abundance <- total_abundance + abundance_sample[[i]]
      if (total_abundance >= hover$y) {
        index_taxonomy <- i
        break
      }
    }
    if (index_taxonomy == -1) {
      return(NULL)
    } else{
      wellPanel(style = style,
            tags$b("Sample: "),
            sample,
            br(),
            tags$b("Taxonomy: "),
            as.character(taxonomy_sample[[index_taxonomy]]),
            br(),
            tags$b("Abundance: "),
            abundance_sample[[index_taxonomy]]
       )
    }
  })
  # this will show the table with details  
  observeEvent(input$plot_click, {
    click <- input$plot_click
    if (is.null(click$x))
      return(NULL)
    df <- data()
    lvls <- levels(df$Sample)
    sample <- lvls[round(click$x)]
    selected_sample = subset(df, df$Sample == sample)
    output$sample_subset <- renderDataTable(selected_sample,options = list(aaSorting = list(list(2, 'desc'),list(1, 'asc'))) )
  })
})
