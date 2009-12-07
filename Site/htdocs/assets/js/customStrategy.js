// Hacks to keep EuPath family-specific functionality in place

// Initialize blast question in step popups on all sites
function customGetQueryForm(questionName, url) {
   if(questionName.indexOf("BySimilarity") != -1){
      initBlastQuestion(url);
   }
}


// Initialize blast & ortholog questions in edit step
// popups on all sites
function customEditStep(questionName, url) {
   if(questionName.indexOf("BySimilarity") != -1){
      initBlastQuestion(url);
   } else if (questionName.indexOf("OrthologPattern") != -1){
      initOrthologQuestion(url);
   }
}

// Include orthologs link in gene details boxes in all sites
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

function showInstructions(){
	$("#strat-instructions").remove();
	$("#strat-instructions-2").remove();
	$("#Strategies").removeAttr("style"); // DO NOT DELETE.  This is for IE.
	var instr = document.createElement('div');
	id = "strat-instructions";
	instr_text = "<br>Click '<a href='queries_tools.jsp'>New Search</a>' <br/> to start a strategy";
	instr_text2 = "Or Click on '<a href=\"javascript:showPanel('search_history')\">Browse Strategies</a>' to view your available strategies.";
	arrow_image = "<img id='ns-arrow' alt='Arrow pointing to New Search Button' src='wdk/images/lookUp.png' width='45px'/>"; 
	arrow_image2 = "<img id='bs-arrow' alt='Arrow pointing to Browse Strategy Tab' src='wdk/images/lookUp2.png' width='45px'/>"; 
	as = $("#mysearch").text();
	as = as.substring(as.indexOf(":") + 2);
	if(as != "0"){
		instr_text = instr_text + "<br>" + instr_text2;
		id = id + "-2";
		arrow_image = arrow_image + arrow_image2;
	}
	$(instr).attr("id",id).html(arrow_image + instr_text);
	$("#Strategies").css({'overflow' : 'visible'}); // DO NOT DELETE.  This is for IE to display instructions correctly.
	$("#Strategies").append(instr);
}

function showBasket(sig){
	//showSummary.do?
	//	questionFullName=InternalQuestions.GeneRecordClasses_GeneRecordClassByBasket&
	//	myProp%28user_signature%29=ab4d3f5d505335cdd529be306fc28ceb&
	//	resultsOnly=true&
	//	myProp%28timestamp%29=1
	
	var url = "showSummary.do";
	var d = new Object();
	d.questionFullName = "InternalQuestions.GeneRecordClasses_GeneRecordClassByBasket";
	d.user_signature = sig;
	d.resultsOnly = true;
	d.timestamp = 1;
//	d.checksum = "18837a74129e71e5eef724f59e0a01c9";
//	d.noskip = 1;
//	d.resultsOnly = true;
//	d.step = 1;
//	d.strategy = 1;
	$.ajax({
		url: url,
		data: d,
		type: "post",
		dataType: "html",
		success: function(data){
			$("div#basket").html(data);
		},
		error: function(data,msg,e){
			alert("Error occured in showBasket() function!!");
		}
	});
}
