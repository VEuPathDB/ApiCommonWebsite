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
outputFile <-args[2]

library(GOsummaries);
up <-read.table(data, header =TRUE, sep ="\t", quote="");
up <- head(up, 40);  # Limit to top 40 (library default)

# update 6/30/2026
# limit to top 40.  previously including all, which produced garbage
# no longer use GOsummaries.plot().  it offers no particular value, and hard-codes poor config settings
# instead, use the low-level method it calls.  Luckily GOsummmaries exposes it.  No need to install a different R package

# Remove NAs
up <- up[!is.na(up$Pvalue), ]

# Handle zero p-values (replace with minimum non-zero)
min_pval <- min(up$Pvalue[up$Pvalue > 0])
up$Pvalue[up$Pvalue == 0] <- min_pval

png(outputFile, width=3000, height=1000)
plotWordcloud(words=up$Name,
              freq=-log10(up$Pvalue), 
              scale=3.0,          # BIGGER fill
              rot.per=0,              # DISABLE VERTICAL TEXT (default is 0.3)
              algorithm="leftside", 
              min.freq=-Inf,
              random.order=FALSE)

dev.off();
