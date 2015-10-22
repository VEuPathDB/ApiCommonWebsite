#set working directory
setwd("E:/Users/San/Desktop/Temp/R/shiny/App-11-cv")

library(rCharts)
library(reshape2)
library(zoo)

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
d <- LTS.D[order(as.yearmon(LTS.D$DateOfVisit, "%y-%b")),]
head(d)
View(d)
rPlot(AnophelesCollectedtotal~DateOfVisit, data = d, type = 'bar', color ='GeographicLocation', facet='GeographicLocation' ) #Plot PD as it depends on MonthYear
rPlot(AnophelesCollectedtotal~DateOfVisit, data = LTS.D, type = 'line', color ='GeographicLocation', facet='GeographicLocation' ) #Plot PD as it depends on MonthYear
#data(economics, package = "ggplot2")
#econ <- transform(economics, date = as.character(date))
# m1 <- mPlot(x = "DateOfVisit", y = c("TotalAnophelesPositive","TotalAnophelesTested"), type = "Bar", data = LTS.D)
# m1$set(pointSize = 0, lineWidth = 1)
# m1

monyr <- as.yearmon(unique(LTS.D$DateOfVisit), "%y-%b")
class(monyr)
head(monyr)
monyr <- monyr[order(monyr)]
head(LTS.D$DateOfVisit)
