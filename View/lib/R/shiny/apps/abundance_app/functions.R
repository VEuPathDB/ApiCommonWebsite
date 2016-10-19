# This function receives an array with different taxonomic levels and return a string in the Qiime format:
# k__Kingdom;p__Phylum;c__Class;o__Order;f__Family;g__Genus;s__Species
format.taxonomy <- function(tax.names){
  formatted.taxonomy <- ""
  level.names <- c("k__", "p__", "c__", "o__", "f__", "g__", "s__" )
  if(length(tax.names) > 1){
    formatted.taxonomy <- sprintf("%s%s", level.names[1], tax.names[1])
    for(i in 2:length(tax.names)){
      formatted.taxonomy <- paste0(formatted.taxonomy,"; ",level.names[i],tax.names[i])
    }
  } else if(length(tax.names) == 1){
    formatted.taxonomy <- paste0(level.names[1], tax.names[1])
  }
  else{
   return(NULL); 
  }
  formatted.taxonomy
}

# output$hover_info <- renderUI({
#   hover <- input$plot_hover
#   if (is.null(hover$x))
#     return(NULL)
#   
#   left_pct <-
#     (hover$x - hover$domain$left) / (hover$domain$right - hover$domain$left)
#   top_pct <-
#     (hover$domain$top - hover$y) / (hover$domain$top - hover$domain$bottom)
#   
#   left_px <-
#     hover$range$left + left_pct * (hover$range$right - hover$range$left)
#   top_px <-
#     hover$range$top + top_pct * (hover$range$bottom - hover$range$top)
#   
#   style <-
#     paste0(
#       "position:absolute; z-index:100; background-color: rgba(245, 245, 245, 0.85); "
#       ,
#       "left:",
#       left_px + 2,
#       "px; top:",
#       top_px + 2,
#       "px;"
#     )
#   df <- data()
#   lvls <- levels(df$Sample)
#   sample <- lvls[round(hover$x)]
#   selected_sample = subset(df, df$Sample == sample)
#   abundance_sample <- selected_sample$Abundance
#   taxonomy_sample <- selected_sample$Taxonomy
#   total_abundance <- 0
#   index_taxonomy <- -1
#   for (i in seq_along(abundance_sample)) {
#     total_abundance <- total_abundance + abundance_sample[[i]]
#     if (total_abundance >= hover$y) {
#       index_taxonomy <- i
#       break
#     }
#   }
#   if (index_taxonomy == -1) {
#     return(NULL)
#   } else{
#     wellPanel(style = style,
#               p(HTML(
#                 paste0(
#                   "<b> Sample: </b>",
#                   sample,
#                   "<br/>",
#                   "<b> Taxonomy: </b>",
#                   as.character(taxonomy_sample[[index_taxonomy]]),
#                   "<br/>",
#                   "<b> Abundance: </b>",
#                   abundance_sample[[index_taxonomy]],
#                   "<br/>"
#                 )
#               )))
#   }
# })