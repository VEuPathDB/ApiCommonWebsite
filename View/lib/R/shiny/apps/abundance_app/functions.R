get_abundance_index <- function(array_search, abundance_hover){
  index_found = 0
  if(abundance_hover < 0)
    return(-1)
  for (i in 1:length(array_search)) {
    if(i == 1){
      if(abundance_hover <= array_search[1]){
        index_found=1
        break
      }
    }else if(i < length(array_search)){
      if(abundance_hover > array_search[i-1] & abundance_hover <= array_search[i]){
        index_found=i
        break
      }
    }else{
      if(abundance_hover <= array_search[length(array_search)]){
        index_found=length(array_search)
      }else{
        index_found=-1;
      }
    }
  }
  index_found
}

get_abundances_from_plot <- function(array_abundance){
  array_real_abundance <- 0
  array_real_abundance[1] <- array_abundance[1]
  for (i in 2:length(array_abundance)) {
    diff_abundance <- array_abundance[i]-array_abundance[i-1]
    if(diff_abundance > 0){
      array_real_abundance[i] <- array_abundance[i]-array_abundance[i-1]
    }else{
      break
    }
  }
  array_real_abundance
}

join_abundance <- function(array_abundance, data_from_chart){
  df_to_return <- data.frame()
  for(i in 1:length(array_abundance)){
    abi <- array_abundance[i]
    line_to_remove <- 0
    for(j in 1:nrow(data_from_chart)){
      abj <- data_from_chart[j,"Abundance"]
      if(isTRUE(all.equal(abi,abj))){
        df_to_return <- rbind(df_to_return, data_from_chart[j,])
        line_to_remove = j
        break
      }
    }
    if(line_to_remove > 0){
      data_from_chart <- data_from_chart[-c(line_to_remove), ]
    }
  }
  df_to_return
}


