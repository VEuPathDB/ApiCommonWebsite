# Declaring the packages
library(shiny)
library(shinyjs)

appCSS <- "
#loading-content {
position: absolute;
background: #FFFFFF;
opacity: 0.9;
z-index: 100;
left: 0;
right: 0;
height: 100%;
text-align: center;
color: #858585;
}
"

# The shiny ui function build the HTML final page showed in the shiny app
shinyUI(
  fluidPage(
    useShinyjs(),
    inlineCSS(appCSS),
    # Loading message
    div(id = "loading-content",
        h5("We are preparing the graphical representation..."),
        img(src = "loading.gif")
    ),
    # The main app code goes here
    hidden(
    div(
    id = "app-content",
         fluidRow(column(
           3,
           selectInput(
             "taxonLevel",
             label = "Taxonomic level: ",
             choices = c("Phylum", "Class", "Order", "Family", "Genus", "Species"),
             selected = "Phylum",
             width = '100%'
           ),
           # this div is not showed, this is just a workaround to load the files in a reactive environment
           div(style = "display: none;",
               checkboxInput(
                 "taxa_are_rows", label = "", value = T
               ))
         ),
         column(
           9,
           selectizeInput(
             "category",
             choices = NULL,
             label = "Split by the category:",
             options = list(placeholder = 'Loading...'),
             width = "100%"
           )
         )),
         
         tabsetPanel(
           id = "tabs",
           tabPanel(
             title = "By Sample",
             value = "bySample",
             fluidRow(column(
               12,
               div(style = "position:relative",
                   uiOutput("abundanceChart"),
                   uiOutput("hover_info"))
             )),
             fluidRow(column(
               12,
               dataTableOutput("by_sample_datatable")
             ))
           ),
           tabPanel(
             title = "By OTU",
             value = "byOTU",
             fluidRow(column(
               12,
               selectizeInput(
                 "filterOTU",
                 choices = NULL,
                 label = "Filter by OTU:",
                 options = list(placeholder = 'Loading...'),
                 width = "100%"
               )
             )),
             fluidRow(column(12,
                        uiOutput("chartByOTU"),
                        uiOutput("hoverByOTU")
                      )
                    ),
             fluidRow(column(
               12,
               dataTableOutput("by_otu_datatable")
             ))
           ) # end tabPabel byOTU
         ) # end tabSetPanel
    ) # end div id = "app-content",
    ) # end hidden
  ) # end fluidPage
) # end shinyUI