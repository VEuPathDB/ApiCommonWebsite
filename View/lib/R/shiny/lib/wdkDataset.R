library(shiny)

# A function that fetches the passed contextHash or ID of a
# WDK step analysis, loads the data file output into a dataset,
# and returns it.  If fetchStyle is "disk", file is read
# directly from the step analysis storage dir (passed in). If
# fetchStyle is "url", data is read from URL.

getWdkDataset <- function(session, fetchStyle, expectHeader, dataStorageDir="") {

  query = parseQueryString(session$clientData$url_search)

  if (fetchStyle == "disk") {
    contextHash = get("contextHash", query)
    validate(
      need(contextHash != "", "Must pass a contextHash query parameter"),
      need(grepl("..",contextHash), "Hash must be sent alone")
    )
    dataFile <- paste0(dataStorageDir, "/", contextHash, "/data.tab")
    print(paste0("Will read from: ", dataFile), stderr())
    read.table(dataFile, sep="\t", header=FALSE)
  }
  else {
    fetchUrl = get("dataUrl", query)
    validate(
        need(fetchUrl != "", "Must pass a dataUrl query paramenter")
    )
    print(paste0("Will read from: ", fetchUrl), stderr())
    read.table(fetchUrl, sep="\t", header=expectHeader)
  }
}
