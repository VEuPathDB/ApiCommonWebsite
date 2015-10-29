#set working directory
#setwd("E:/Users/San/Desktop/Temp/R/shiny/App-3-lts")
source("../../lib/wdkDataset.R")
source("config.R")

library(reshape2)
library(rCharts)

LTS <- read.csv("LTS.csv", sep = ",", as.is = T, na.strings = (list("null"))) #import data

str(LTS)
names(LTS) <- substr(names(LTS),3,50) #Remove preceeding 'x.'
names(LTS) = gsub("\\.", "", names(LTS)) #Remove remaining periods

#coerce variables to required type:
LTS$GeographicLocation <- as.factor(LTS$GeographicLocation)

LTS.S <- subset(LTS, AnophelesCollectedtotal >0, select = c("DateOfVisit","AnophelesCollectedtotal","GeographicLocation",
                                                            "TotalAnophelesPositive","TotalAnophelesTested","Parous",
                                                            "Nulliparous","AFunestus","AGambiae","OtherAnopheles","CollectionBarcode"))
#head(LTS.S)

#unique(sx$DateOfVisit)
LTS.M <- melt(LTS.S, id.vars =c("DateOfVisit","CollectionBarcode","GeographicLocation") )
#head(LTS.M)
#str(LTS.M)

#LTS$DateOfVisit <- as.character(LTS$DateOfVisit)
#str(LTS$DateOfVisit)
LTS.D <- dcast(LTS.M,DateOfVisit+GeographicLocation~variable, fun.aggregate = sum)
#head(LTS.D)
#str(LTS.D)
#compute max dates
max.d <- max(LTS.D$DateOfVisit)
min.d <- min(LTS.D$DateOfVisit)
#rPlot(AnophelesCollectedtotal~DateOfVisit, data = LTS.D, type = 'bar', color ='GeographicLocation', facet='GeographicLocation' ) #Plot PD as it depends on MonthYear

