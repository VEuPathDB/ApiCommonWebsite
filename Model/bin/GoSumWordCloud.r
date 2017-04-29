#!/usr/bin/Rscript
# Command line arguments for R are not fun... so to make this easy create the args
# by prepending them to this script directly before the R call:
# [bash]$ echo 'arg1 = "value";arg2="someValue"' |cat - script.r | R --no-save
# Variables dataFrame, inputDir, outputDir,  are required
args <- commandArgs(TRUE)

if (length(args) != 2) {
    print("Usage: GoSumWordCloud.r <data> <outputFile>")
}

data <- args[1]
#inputDir <- args[2]
#outputDir <-args[3]
outputFile <-args[2]

library(GOsummaries);
up <-read.table(data, header =TRUE, sep ="\t");
wcd1 = data.frame(Term = up$Name, Score = up$Pvalue);
gs = gosummaries(wc_data = list(wcd1));
#DO I NEED TO GIVE IT AN OUTPUT DIR?
#fullOutFile = paste(outputDir, outputFile, sep="/");
#plot(gs, filename ="fullOutFile");


#IF NOT THEN:

plot(gs, filename =outputFile);



#library(ggplot2);
#up <-read.table(data, header =TRUE, sep ="\t");
#png(outputFile);
#hist(up$Result.count);
dev.off();
