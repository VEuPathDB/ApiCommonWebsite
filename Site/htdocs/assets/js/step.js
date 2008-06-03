$("#diagram").ready(function(){
/*	
	$(".crumb_name").mouseover(function(){
		var detail = $(this).parent().siblings(".crumb_details");
		detail.addClass("myhover");
	});
	
	$(".crumb_detail").mouseout(function(){
		var detail = $(this).parent().siblings(".crumb_details");
		detail.removeClass("myhover");
	});
*/
	 
/*	$(".crumb_name").mouseout(function(){
		var detail = $(this).parent().siblings(".crumb_details");
		detail.removeClass("myhover");		
	});
*/
	$(".crumb").click(function(){
		var a = $(this).children("h3").children("a");
		a.click();
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

