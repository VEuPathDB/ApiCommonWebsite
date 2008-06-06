
function closeAll(){$("#filter_link").click();}

function formatFilterForm(data, edit, reviseStep){
	//edit = 0 ::: adding a new step
	//edit = 1 ::: editing a current step
				
				var stepn = 0;
				if(reviseStep != ""){
					var parts = reviseStep.split(":");
					stepn = parts[1];
					reviseStep = parts[0];
					isSub = parts[2];
				}
				var proto = $("#proto").text();
				var pro_url = "";
			if(edit == 0)
				pro_url = "processFilter.do?protocol=" + proto;
			else{
				
				pro_url = "processFilter.do?protocol=" + proto + "&revise=" + reviseStep + "&step=" + stepn + "&subquery=" + isSub;
			}
				var historyId = $("#history_id").val();
				var stepNum = $("#target_step").val() - 1;
			if(edit == 0)
				var close_link = "<a id='close_filter_query' href='javascript:close()'>close[x]</a>";
	 		else
				var close_link = "<a id='close_filter_query' href='javascript:closeAll()'>close[x]</a>";
				var quesTitle = $("h1",data).text().replace(/Identify Genes based on/,"");
				var quesForm = $("form",data);

				$("input[value=Get Answer]",quesForm).val("Run Step");
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
				$(".filter.params", quesForm).prepend("<span class='form_subtitle'>Add&nbsp;Step&nbsp;" + (stepNum + 1) + ": " + quesTitle + "</span></br>");
				//$(".filter.params", quesForm).prepend("<span class='form_subtitle'>" + quesTitle + "</span><br>"); 

				$(".filter.params", quesForm).after("<div class='filter operators'><span class='form_subtitle'>Combine with Step " + stepNum + "</span><div id='operations'><ul><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " AND' checked='checked'/><li class='operation INTERSECT'/><li>&nbsp;" + stepNum + "&nbsp;<b>INTERSECT</b>&nbsp;" + (stepNum + 1) + "</li><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " OR'><li class='operation UNION'/><li>&nbsp;" + stepNum + "&nbsp;<b>UNION</b>&nbsp;" + (stepNum + 1) + "</li><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " NOT'></li><li class='operation MINUS'/><li>&nbsp;" + stepNum + "&nbsp;<b>MINUS</b>&nbsp;" + (stepNum + 1) + "</li></ul></div></div>");

//				var action = quesForm.attr("action").replace(/processQuestion.do/,"processFilter.do?protocol=" + proto);
				var action = quesForm.attr("action").replace(/processQuestion.do/,pro_url);

				quesForm.prepend("<hr style='width:99%'/>");
				quesForm.prepend("<h1>Add&nbsp;Step</h1>");
				//quesForm.prepend("<h1>Add&nbsp;Step&nbsp;" + (stepNum + 1) + "</h1>");

				quesForm.attr("action",action);
				$("#query_form").html(close_link);
				$("#query_form").append("<img class='dragHandle' src='images/HAND.png'/>");
				$("#query_form").append(quesForm);
				$("#query_selection").fadeOut("normal");
				$("#query_form").css({
					top: "337px",
					left: "22px"
				});
				$("#query_form").jqDrag(".dragHandle");
				$("#query_form").fadeIn("normal");
}


$(document).ready(function(){

	$("div.crumb_details").hide();
	$("#filter_div").hide();
	$("#query_form").hide();
	$(".top_nav ul li a").click(function(){
		
		var url = $(this).attr("href");
		//$.get(url, function(data){
		//		$("#query_form").html(data);
		//	}
		//);
		$.ajax({
			url: url,
			dataType:"html",
			success: function(data){
				formatFilterForm(data,0,"");
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
		$("#instructions").text("");
		return false;
	});
	
	$("#filter_link").click(function(){;
		if($(this).text() == "Add Step"){
			$("#filter_div").fadeIn("normal");
			$(this).html("<span>Cancel [X]</span>"); 
	}else{
			$("#filter_div").fadeOut("normal");
			$("#query_selection").show();
			$("#query_form").hide();
			$(this).html("<span>Add Step</span>"); 
	}
	});

	$(".crumb").bind("mouseenter",function(){
		$(".crumb_details",this).fadeIn("fast");
	}).bind("mouseleave",function(){
		$(".crumb_details",this).fadeOut("fast");
	});

//	$(".crumb_details").mouseover(function(){
//		$(this).filter(".crumb_detail").show();
//	}).mouseout(function(){
//		$(this).hide();
//	});
	
}); // End of Ready Function

function close(){
	$("#query_form").fadeOut("normal");
	$("#query_selection").fadeIn("normal");
	$("#instructions").text("Revise your results by adding steps from the list below.");
}
