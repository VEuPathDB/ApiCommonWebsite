#!/usr/bin/env Rscript

library(shiny)

# get app dir name from args
args <- commandArgs(trailingOnly=TRUE)
appName <- args[1]
if (is.na(appName)) {
  print("USAGE: startup.R <app_dir>")
  q()
}

# source this app's config.R to get the webapp port
print("Reading app's config.R to retrieve webappPort")
source(paste0("apps/",appName,"/config.R"))

# run the app
runApp(paste0("apps/",appName), webappPort, host="0.0.0.0")
