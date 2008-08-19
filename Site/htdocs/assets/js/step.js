$("#diagram").ready(function(){

//	$("a.crumb_name, a.operation, a.view_step_link").click(function(){
//		NewResults($(this)[0]);
//		return false;
//	});

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
		element = $("div#diagram div.box:last");
		var n = element.attr("id");
		n = parseInt(n.substring(5));
		if(n == 0)
			className = "selectedarrow";
		else
			className = "selected";
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

function showDetails(det){
	det = $(det).parent().parent().find("h3 div.crumb_details");
	var disp = det.css("display");
	var crumb_details = $("#diagram h3 div.crumb_details");
	for(i=0;i<crumb_details.length;i++){
		if($(crumb_details[i]).css("display") == "block")
			$(crumb_details[i]).css("display", "none");
	}
	if(disp == "none")
		det.show();
	else
		det.hide();
}

function hideDetails(det){
	det = $(det).parent().parent().parent();
	det.hide();
}


function NewResults(ele,url){
	var classname = "selectedarrow";
	if($(ele).hasClass("operation"))
		classname = "selected";
	//var url = $(ele).attr("value");
	//var url = ele.attr("value");
	$.ajax({
		url: url,
		dataType: "html",
		success: function(data){
			$("div#Workspace").html(data);
		},
		error : function(data, msg, e){
			  alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	$("div.selectedarrow").removeClass("selectedarrow");
	$("div.selected").removeClass("selected");
	if($(ele).hasClass("crumb_name")){
		$(ele).parent().parent().addClass(classname);
		$(ele).siblings("div.crumb_details").hide();
	}
	else if($(ele).hasClass("operation")){
		$(ele).parent().addClass(classname);
	}
	else if($(ele).hasClass("view_step_link")){
		$(ele).parent().parent().parent().parent().addClass(classname);
		$(ele).parent().parent().hide();	
	}
}

function Edit_Step(ele,url){
		$(ele).parent().parent().hide();
		var link = $("#filter_link");
		$(link).css({opacity:0.2});//html("<span>Close [X]</span>");
		$(link).attr("href","javascript:void(0)");
		hideDetails();
		var revisestep = $(ele).attr("id");
		var parts = revisestep.split("|");
		revisestep = parseInt(parts[0]);
		var operation = parts[1];
		var reviseStepNumber = revisestep + ":0:0:" + operation;
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
		$(this).parent().parent().hide();
}

function Insert_Step(ele,url){
	$(ele).parent().parent().hide();
	var sNumber = $(ele).parent().parent().parent().parent().attr("id");
	sNumber = sNumber.substring(5,6);
	openFilter(sNumber);
}

function Rename_Step(ele){
	$(ele).parent().parent().hide();
	var link = $(ele).parent().parent().parent().find("a:first");
	link.html("<input id='new_name_box' type='text' value='"+link.text()+"' onblur='RenameStep(this)' onfocus='this.select()' onkeypress='blah(this,event)' size='10'/>");
	$("#new_name_box").focus();
}

function RenameStep(ele){
	var a = $(ele).parent();
	var new_name = $(ele).val();
	var x = $(ele).parent().attr("id");
	x = x.substring(7);
	var url = "renameUserAnswer.do?user_answer_id=" + x + "&customUserAnswerName=" + new_name;	
	if(new_name.length > 14)
		new_name = new_name.substring(0,12) + "...";	
	$.ajax({
			url: url,
			dataType: "html",
			success: function(data){
				a.text(new_name);
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
}

// Utility Functions

function blah(ele,evt){
	var charCode = (evt.which) ? evt.which : evt.keyCode;
	if(charCode == 13) $(ele).blur();
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


