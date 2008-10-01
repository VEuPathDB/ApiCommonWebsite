
var isInsert = "";
var _action = "";
var original_Query_Form_Text;

//$(document).ready(function(){
//	original_Query_Form_Text = $("#query_form").html();	
//});  End of Ready Function

function closeAll(){openFilter(isInsert);}

function formatFilterForm(data, edit, reviseStep){
	//edit = 0 ::: adding a new step
	//edit = 1 ::: editing a current step
	var operation = "";
	var stepn = 0;
	var insert = "";
	var proto = "";
	if(edit == 0){
		var parts = reviseStep.split(":");
		insert = parts[1];
		proto = parts[0];
	}else{
		var parts = reviseStep.split(":");
		proto = parts[0];
		reviseStep = parseInt(parts[1]);
		isSub = true;
		operation = parts[4];
	}
	//var lastStepId = $("#last_step_id").text();
	var lastStepId = $("#diagram_" + proto + " div.venn:last").attr("id");
	lastStepId = lastStepId.substring(5);
	
	var pro_url = "";
	if(edit == 0)
		pro_url = "processFilter.do?strategy=" + proto + "&insert=" +insert;
	else{
		pro_url = "processFilter.do?strategy=" + proto + "&revise=" + reviseStep;// + "&step=" + stepn + "&subquery="; + isSub;
	}
	var historyId = $("#history_id").val();
	
	//var stepNum = $("#target_step").val();
	var stepNum = $("#diagram_" + proto + " div.box:last h3 a,crumb_name").attr("id");
	stepNum = stepNum.substring(7);
	
	stepNum = parseInt(stepNum) + 1;
	var prev_stepNum = stepNum - 1;
	if(edit == 0){
		var close_link = "<a id='close_filter_query' href='javascript:closeAll()'><img src='/assets/images/Close-X-box.png'/></a>";
		var back_link = "<a id='back_to_selection' href='javascript:close()'><img src='/assets/images/backbox.png'/></a>";
	}else
		var close_link = "<a id='close_filter_query' href='javascript:closeAll()'><img src='/assets/images/Close-X-box.png'/></a>";
	var quesTitle = $("h1",data).text().replace(/Identify Genes based on/,"");
	var quesForm = $("form#form_question",data);
	$("input[value=Get Answer]",quesForm).val("Run Step");
	$("input[value=Run Step]",quesForm).attr("id","executeStepButton");
	$("div:last",quesForm).attr("align", "");
	$("div:last",quesForm).attr("style", "float:left;margin: 45px 0 0 1%;");
    $("table:first", quesForm).wrap("<div class='filter params'></div>");
	$("table:first", quesForm).attr("style", "margin-top:15px;");
	// Bring in the advanced params, if exist, and remove styling
	var advanced = $("#advancedParams_link",quesForm);
	advanced = advanced.parent();
	advanced.remove();
	advanced.attr("style", "");
	$(".filter.params", quesForm).append(advanced);
	if(edit == 0)
		$(".filter.params", quesForm).prepend("<span class='form_subtitle'>Add&nbsp;Step&nbsp;" + stepNum + ": " + quesTitle + "</span></br>");
	else
		$(".filter.params", quesForm).prepend("<span class='form_subtitle'>Edit&nbsp;Step&nbsp;" + (reviseStep + 1) + ": " + quesTitle + "</span></br>");
	if(edit == 0){
		var previous_step_id = $("#step_"+prev_stepNum+"_sub a").attr("id");
		$(".filter.params", quesForm).after("<div class='filter operators'><span class='form_subtitle'>Combine Step " + prev_stepNum + " with Step " + stepNum + "</span><div id='operations'><table><tr><td class='opcheck' valign='middle'><input type='radio' name='myProp(booleanExpression)' value='AND' /></td><td class='operation INTERSECT'></td><td valign='middle'>&nbsp;" + prev_stepNum + "&nbsp;<b>INTERSECT</b>&nbsp;" + stepNum + "</td></tr><tr><td class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='OR'></td><td class='operation UNION'></td><td>&nbsp;" + prev_stepNum + "&nbsp;<b>UNION</b>&nbsp;" + stepNum + "</td></tr><tr><td class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='NOT'></td><td class='operation MINUS'></td><td>&nbsp;" + prev_stepNum + "&nbsp;<b>MINUS</b>&nbsp;" + stepNum + "</td></tr></table></div></div>");
	} else {
		if(reviseStep != 0){
			if(reviseStep != 1)
				var previous_step_id = $("#step_"+(reviseStep - 1)+"_sub a").attr("id");
			else
				var previous_step_id = $("#step_"+(reviseStep - 1)+" a").attr("id");						
			lastStepId = previous_step_id.substring(7);
			$(".filter.params", quesForm).after("<div class='filter operators'><span class='form_subtitle'>Combine with Step " + (reviseStep) + "</span><div id='operations'><table><tr><td class='opcheck'><input id='INTERSECT' type='radio' name='myProp(booleanExpression)' value='AND' /></td><td class='operation INTERSECT'></td><td>&nbsp;" + (reviseStep) + "&nbsp;<b>INTERSECT</b>&nbsp;" + (reviseStep + 1) + "</td></tr><tr><td class='opcheck'><input id='UNION' type='radio' name='myProp(booleanExpression)' value='OR'></td><td class='operation UNION'></td><td>&nbsp;" + (reviseStep) + "&nbsp;<b>UNION</b>&nbsp;" + (reviseStep + 1) + "</td></tr><tr><td class='opcheck'><input id='MINUS' type='radio' name='myProp(booleanExpression)' value='NOT'></td><td class='operation MINUS'></td><td>&nbsp;" + (reviseStep) + "&nbsp;<b>MINUS</b>&nbsp;" + (reviseStep + 1) + "</td></tr></table></div></div>");
		}else{
			$(".filter.params", quesForm).after("<input type='hidden' name='myProp(booleanExpression)' value='AND' />");
		}
	}
	if(edit == 0)	
		var action = "javascript:AddStepToStrategy('" + pro_url + "')";
	else
		var action = "javascript:EditStep('" + pro_url + "', " + parseInt(reviseStep) + ")";
	var formtitle = "";
	if(edit == 0)
		formtitle = "<h1>Add&nbsp;Step</h1>";
	else
		formtitle = "<h1>Edit&nbsp;Step</h1>";
	quesForm.attr("action",action);
	if(edit == 0)
		var header = "<span class='dragHandle'>" + back_link + " " + formtitle + " " + close_link + "</span>";
	else
		var header = "<span class='dragHandle'>" + formtitle + " " + close_link + "</span>";
		
	$("#filter_link_div_" + proto + " #query_form").html(header);
	$("#filter_link_div_" + proto + " #query_form").append(quesForm);
	$("#filter_link_div_" + proto + " #query_selection").fadeOut("normal");
	if(edit == 1)
		$("#filter_link_div_" + proto + " #query_form div#operations input#" + operation).attr('checked','checked'); 
	$("#filter_link_div_" + proto + " #query_form").jqDrag(".dragHandle");
	$("#filter_link_div_" + proto + " #query_form").fadeIn("normal");
}

function getQueryForm(url){	
		$.ajax({
			url: url,
			dataType:"html",
			success: function(data){
				formatFilterForm(data,0,isInsert);
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
}

function openFilter(IsIn) {
	//if(IsIn.indexOf("add") != -1)
	//	isInsert = "";
	//else
	var strat_number = IsIn.split(":");
	strat_number = strat_number[0]; 
	isInsert = IsIn;
	var link = $(".filter_link");
	if($(link).attr("href").indexOf("openFilter") != -1) {   //"javascript:openFilter('add')"){
//		$("#filter_div").fadeIn("normal");
		original_Query_Form_Text = $("#filter_link_div_" + strat_number + " #query_form").html();
		$("#filter_link_div_" + strat_number + " #query_form").css({
			top: "337px",
			left: "22px"
		});
		$("#filter_link_div_" + strat_number + " #query_form").show("normal");
		$("#filter_link_div_" + strat_number + " #query_form").jqDrag(".dragHandle");
		for(var i=0; i < link.length; i++){
			$(link[i]).css({opacity:0.2});//html("<span>Close [X]</span>");
			$(link[i]).attr("value",$(link[i]).attr("href"));
			$(link[i]).attr("href","javascript:void(0)");
		}
	}else{
		//$("#filter_div").fadeOut("normal");
		//$("#query_selection").show();
		$("#filter_link_div_" + strat_number + " #query_form").html(original_Query_Form_Text);
		$("#filter_link_div_" + strat_number + " #query_form").hide();
		for(var i=0; i < link.length; i++){
			$(link[i]).css({opacity:1.0});//html("<span>Add Step</span>"); 
			$(link[i]).attr("href",$(link[i]).attr("value"));
		}
	}
}

function close(){
	var strat_number = isInsert.split(":");
	strat_number = strat_number[0]; 
	$("#filter_link_div_" + strat_number + " #query_form").html(original_Query_Form_Text);//fadeOut("normal");
	$("#filter_link_div_" + strat_number + " #query_form").jqDrag(".dragHandle");
}

function parseInputs(){
	var quesForm = $("form[name=questionForm]");
	var inputs = $("input", quesForm);
	var selects = $("select", quesForm);
	var d = "";
	var isFirst = 1;
	for(i=0;i<inputs.length;i++){
	    var name = inputs[i].name;
	    if(inputs[i].type == "checkbox" || inputs[i].type == "radio"){
		var boxType = inputs[i].type;
	    	var tempName = name;
		var tempValue = "";
		while(tempName == name && inputs[i].type == boxType){
		   if(inputs[i].checked == true)
			tempValue = tempValue + "," + inputs[i].value;
		   i++;
		   name = inputs[i].name;
		}
		tempValue = tempValue.substring(1);
		if(d == "")
			d = tempName + "=" + tempValue;
		else
			d = d + "&" + tempName + "=" + tempValue;
		i--;
	    }else{
	      if(inputs[i].type != "submit"){
	    	var value = inputs[i].value;
	    	if(i == 0)
			d = name + "=" + value;
	    	else
			d = d + "&" + name + "=" + value;
	      }
	    }
	    isFirst = 0;
	}
	for(i=0;i<selects.length;i++){
			var sname = selects[i].name;
			var svalue = selects[i].value;
			if(isFirst == 1)
				d = sname + "=" + svalue;
		    else
				d = d + "&" + sname + "=" + svalue;
	}
	return d;
}	

function InsertNewStrategy(data){
	var new_dia_id = $(".diagram",data).attr("id");
	new_dia_id = "#" + new_dia_id;
	var new_dia = $(".diagram",data);
	$(new_dia_id).html(new_dia.html());
}

function AddStepToStrategy(act){
	var url = act;	
	var d = parseInputs();
	$.ajax({
		url: url,
		type: "POST",
		dataType:"html",
		data: d,
		beforeSend: function(obj){
				var pro_bar = "<div id='step_progress_bar'>" +
							  "<div class='step' id='graphic_span'>Loading...</div></div>";
				$("#loading_step_div").html(pro_bar).show("fast");
				$("#graphic_span").css({opacity: 0.2});
			  for(i = 0;i<100;i++){
				$("#graphic_span").animate({
					opacity: 1.0
				},1000);
				$("#graphic_span").animate({
					opacity: 0.2
				},1000);
			  }
			},
		success: function(data){
			$("#loading_step_div").html("").hide("fast");
			InsertNewStrategy(data);
			var new_dia_id = $(".diagram",data).attr("id");
			$("#" + new_dia_id + " div.venn:last span.resultCount a").click();
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	openFilter(isInsert);
}

function EditStep(url, step_number){
	$("#query_form").hide("fast");
	var d = parseInputs();
		$.ajax({
		url: url,
		type: "POST",
		dataType:"html",
		data: d,
		beforeSend: function(obj){
				var pro_bar = "<div id='step_progress_bar'>" +
							  "<div class='step' id='graphic_span'>Loading...</div></div>";
				$("#loading_step_div").html(pro_bar).show("fast");
				$("#graphic_span").css({opacity: 0.2});
			  for(i = 0;i<100;i++){
				$("#graphic_span").animate({
					opacity: 1.0
				},1000);
				$("#graphic_span").animate({
					opacity: 0.2
				},1000);
			  }
			},
		success: function(data){
			var diagramId = $(".diagram",data).attr("id");
			var diagram_divs = $("#" + diagramId + " div");
			var selected_div = "";
			for(i=0; i < diagram_divs.length;i++){
				var b = $(diagram_divs[i]);
				if($(diagram_divs[i]).hasClass("selectedarrow") || $(diagram_divs[i]).hasClass("selected")){
					selected_div = $(diagram_divs[i]).attr("id");
				}
			}
			$("#loading_step_div").html("").hide("fast");
			InsertNewStrategy(data);
		    $("#"+selected_div+" span.resultCount a").click();
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	$("#filter_link").css({opacity:1.0});//html("<span>Add Step</span>"); 
	$("#filter_link").attr("href","javascript:openFilter()");
}

function DeleteStep(ele,url){
	var deleted_step_id = url.substring(url.indexOf("step=") + 5);
	$.ajax({
		url: url,
		type: "GET",
		dataType:"html",
		beforeSend: function(obj){
				var pro_bar = "<div id='step_progress_bar'>" +
							  "<div class='step' id='graphic_span'>Loading...</div></div>";
				$("#loading_step_div").html(pro_bar).show("fast");
				$("#graphic_span").css({opacity: 0.2});
			  for(i = 0;i<100;i++){
				$("#graphic_span").animate({
					opacity: 1.0
				},1000);
				$("#graphic_span").animate({
					opacity: 0.2
				},1000); 
			  }
			},
		success: function(data){
			var diagramId = $(".diagram",data).attr("id");
			var diagram_divs = $("#" + diagramId + " div");
			var selected_div = "";
			for(i=0; i < diagram_divs.length;i++){
				var b = $(diagram_divs[i]);
				if($(diagram_divs[i]).hasClass("selectedarrow") || $(diagram_divs[i]).hasClass("selected")){
					selected_div = $(diagram_divs[i]).attr("id");
				}
			}
			$("#loading_step_div").html("").hide("fast");
			InsertNewStrategy(data);
			
		    if(selected_div == "step_"+deleted_step_id || selected_div == "step_"+deleted_step_id+"_sub"){
					$("#diagram div.venn:last span.resultCount a").click();
			}else{
				var selected_id = parseInt(selected_div.substring(5,6)) - 1;
				selected_div = selected_div.substring(0,5) + selected_id + selected_div.substring(6);
		    	$("#"+selected_div+" span.resultCount a").click();
			}
		
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
}