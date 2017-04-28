format_abundant_taxa <- function(n_quantity, abundance_taxa){
  if(nrow(abundance_taxa)<=n_quantity){
    return(abundance_taxa)
  }
  
  abundance_taxa<-rbind(abundance_taxa, rep("Remainder", ncol(abundance_taxa)))
  abundance_taxa
}

fix_taxonomy_names <- function(taxonomy_df){
  
  for(i in 1:nrow(taxonomy_df)){
    if(!identical(taxonomy_df[i,1], "N/A")){
      last_defined_taxonomy <- taxonomy_df[i,1]
      for(j in 2:ncol(taxonomy_df[i,])){
        if(identical(taxonomy_df[i,j], "N/A")){
          taxonomy_df[i,j] <- paste("unclassified", last_defined_taxonomy)
        }else{
          last_defined_taxonomy <- taxonomy_df[i,j]
        }
      }
    } 
  }
  taxonomy_df
}

get_n_abundant_overall <- function(n_quantity, abundance_data_frame){
  if(nrow(abundance_data_frame)<=n_quantity){
    return(abundance_data_frame)
  }
  df <- data.frame(row.names = c(rownames(abundance_data_frame)))
  df<-cbind(df, apply(abundance_data_frame, 1, mean))
  colnames(df)<-c("mean")
  setorder(df, -mean)
  df <- head(df, n_quantity)
  df
}

filter_n_abundant <- function(n_quantity, abundance_data_frame){
  if(nrow(abundance_data_frame)<=n_quantity){
    return(abundance_data_frame)
  }
  
  df <- data.frame(row.names = c(rownames(abundance_data_frame),nrow(abundance_data_frame)+1))
  tolerance <- 0.000001
  for(i in 1:length(abundance_data_frame)){
    array_column <- abundance_data_frame[[i]]
    nth_element <- sort(array_column, T)[n_quantity]
    result_column <- array_column>=nth_element|array_column+tolerance>=nth_element|array_column>=nth_element+tolerance
    sum_of_remainder <- sum(array_column[!result_column])
    array_column<-replace(array_column, array_column+tolerance<nth_element, 0)
    array_column[nrow(df)]<-sum_of_remainder
    df<-cbind(df, array_column)
  }
  colnames(df)<-colnames(abundance_data_frame)
  df
}

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
  if(length(array_abundance) > 1){
    for (i in 2:length(array_abundance)) {
      diff_abundance <- array_abundance[i]-array_abundance[i-1]
      if(diff_abundance > 0){
        array_real_abundance[i] <- array_abundance[i]-array_abundance[i-1]
      }else{
        break
      }
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


