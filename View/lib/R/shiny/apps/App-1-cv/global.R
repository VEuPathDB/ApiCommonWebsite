#set working directory
#setwd("E:/Users/San/Desktop/Temp/R/shiny/Ryan/App-1-cv")
source("../../lib/wdkDataset.R")
source("config.R")

cv <- read.csv("visits.csv", sep = ",", as.is = T, na.strings = (list('null'))) #import data

str(cv)
names(cv) <- substr(names(cv),3,50) #Remove preceeding 'x.'
names(cv) = gsub("\\.", "", names(cv)) #Remove remaining periods

#coerce variables to required type:
cv$ClinicalVisitType <- as.factor(cv$ClinicalVisitType)
cv$BloodSmearStatus <- as.factor(cv$BloodSmearStatus)
cv$MalariaDiagnosis <- as.factor(cv$MalariaDiagnosis)
cv$GeographicLocation <- as.factor(cv$GeographicLocation)
cv$BloodSmearReading <- as.factor(cv$BloodSmearReading)
cv$Febrile <- as.factor(cv$Febrile)
cv$Feversubjective <- as.factor(cv$Feversubjective)
cv$SevereMalariaSymptoms <- as.factor(cv$SevereMalariaSymptoms)
cv$Species <- as.factor(cv$Species)