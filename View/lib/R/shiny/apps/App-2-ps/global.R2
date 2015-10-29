#setwd("E:/Users/San/Desktop/Temp/R/shiny/Ryan/App-2-ps")
source("../../lib/wdkDataset.R")
source("config.R")

ps <- read.csv("ps.csv", stringsAsFactors = FALSE, na.strings = "null") #Import data to work with
names(ps) <- substr(names(ps),2,50)#Drop the preceeding 'x's added to the variable names
#labels.ps <- gsub('\\.',' ',names(ps)) 
names(ps) <- gsub('\\.','',names(ps)) #Drop periods inserted into column names by R

# str(ps) #Check variable types and sample values.

#coerce variables in the data frame to desired datatype
#ps$Role <- as.factor(ps$Role)
ps$Sex <- as.factor(ps$Sex)
ps$G6pdGenotype <- as.factor(ps$G6pdGenotype)
ps$HbsGenotype <- as.factor(ps$HbsGenotype)
ps$AthalassemiaGenotype <- as.factor(ps$AthalassemiaGenotype)
ps$GeographicLocation <- as.factor(ps$GeographicLocation)

