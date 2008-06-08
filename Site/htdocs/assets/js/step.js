$("#diagram").ready(function(){
	
	$("div.crumb_menu a.edit_step_link").click(function(){
		$("div#filter_div").show();
		$("#filter_link").html("<span>Cancel [X]</span>");
		hideDetails();
		var url = $(this).attr("href");
		var revisestep = $(this).attr("id");
		var parts = revisestep.split("|");
		revisestep = parseInt(parts[0]);
		var operation = parts[1];
		var currentstep = parseUrl("step");
		var isSub = parseUrl("subquery");
		if(isSub == "" || isSub == "false")
			isSub = "";
		else
			isSub = "true";
		var reviseStepNumber = revisestep + ":" + currentstep + ":" + isSub + ":" + operation;
		$.ajax({
			url: url,
			dataType: "html",
			success: function(data){
				formatFilterForm(data,1,reviseStepNumber);
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
		return false;
	});

	//HIGH LIGHTING CODE
	var stepnumber = parseUrl("step");
	if(stepnumber == "")
		stepnumber = -1;
	else
		stepnumber = stepnumber[0];
	var subquery = parseUrl("subquery");
	if(subquery == 'false' || subquery.length == 0)
		subquery = false;
	else
		subquery = true;
	var className = "";
	var element = "";
	if(stepnumber == -1){
		className = "selected";
		element = $("div#diagram div:last");
	}else{
		if(subquery){
			className = "selectedarrow";
			element = $("div#diagram div#step_" + stepnumber + "_sub");
		}else{
			if(stepnumber == 0)
				className = "selectedarrow";
			else
	 			className = "selected";
		element = $("div#diagram div#step_" + stepnumber);
		}
	}
	element.addClass(className);
	//END HIGH LIGHTING CODE
	
});

var detail_div = "";
overdiv = 0;
function showDetails(det){
	detail_div = $(".crumb_details",det);
	detail_div.show();
}

function hideDetails(){
	if(overdiv == "0") 
		detail_div.hide();
}


function parseUrl(name){
 	name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
 	var regexS = "[\\?&]"+name+"=([^&#]*)";
 	var regex = new RegExp( regexS,"g" );
 	var res = new Array();
 	while (regex.lastIndex < window.location.href.length){
 		var results = regex.exec( window.location.href );
 		if( results != null )
 			res.push(results[1]);
 		else
 			break;
 	}
 	if(res.length == 0)
 		return "";
 	else
 		return res;
}

