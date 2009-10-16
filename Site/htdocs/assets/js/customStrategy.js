function customGetQueryForm(questionName, url) {
	if(questionName.indexOf("BySimilarity") != -1){
		initBlastQuestion(url);
	}
}

function customEditStep(questionName, url) {
	if(questionName.indexOf("BySimilarity") != -1){
		initBlastQuestion(url);
	} else if (questionName.indexOf("OrthologPattern") != -1){
		initOrthologQuestion(url);
	}
}

function customCreateDetails(jsonstep, modelstep) {
	var orthologs = "";
	if(jsonstep.dataType == "GeneRecordClasses.GeneRecordClass"){
		var orthologTgt;
		if (jsonstep.isboolean){
			orthologTgt = modelstep.back_boolean_Id;
		}else{
			orthologTgt = modelstep.back_step_Id;
		}
		orthologs = "<a title='Add an ortholog transform to this step: obtain the ortholog genes to the genes in this result' class='orthologs_link' href='javascript:void(0)' onclick='openOrthologFilter(\"" + strat.backId + "\"," + orthologTgt + ");hideDetails(this)'>Orthologs</a>&nbsp;|&nbsp;";
	}
	return orthologs;
}
