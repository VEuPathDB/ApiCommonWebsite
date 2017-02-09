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