
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
				var proto = $("#proto").text();
				var historyId = $("#history_id").val();
				var stepNum = $("#target_step").val() - 1;
				var close_link = "<a id='close_filter_query' href='javascript:close()'>close[x]</a>";
				var quesTitle = $("b font[size=+3]",data).parent().text().replace(/Identify Genes based on/,"");
				var quesForm = $("form",data);

				$("input[value=Get Answer]",quesForm).val("Run Step");
				$("div:last",quesForm).attr("style", "clear:both;");

                                $("table:first", quesForm).wrap("<div class='filter params'></div>");
				$("table:first", quesForm).attr("style", "margin-top:15px;");

				var advanced = $("#advancedParams_link",quesForm);
				advanced = advanced.parent();
				advanced.remove();
				$(".filter.params", quesForm).append(advanced);
				$(".filter.params", quesForm).prepend("<span class='form_subtitle'>" + quesTitle + "</span><br>"); 

				quesForm.prepend("<div class='filter operators'><span class='form_subtitle'>Combine with Step " + stepNum + "</span><table style='margin-top:15px;left:'><tr><td valign='top' align='right' style='padding: 5px 0 5px 70px;'><b>Operator</b></td><td><div id='operations'><ul><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " AND' checked='checked'/>&nbsp;AND&nbsp</li><li class='operation INTERSECT'/><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " OR'>&nbsp;OR&nbsp;</li><li class='operation UNION'/><li class='opcheck'><input type='radio' name='myProp(booleanExpression)' value='" + proto + " NOT'>&nbsp;NOT&nbsp</li><li class='operation MINUS'/></ul></div></td></tr></table>");

				var action = quesForm.attr("action").replace(/processQuestion.do/,"processFilter.do?protocol=" + proto);
					
				quesForm.prepend("<span id='form_title'>Add A Step</span>");
				quesForm.attr("action",action);
				$("#query_form").html(close_link);
				$("#query_form").append("<img class='dragHandle' src='images/HAND.png'/>");
				$("#query_form").append(quesForm);
				$("#query_selection").fadeOut("normal");
				$("#query_form").css({
					top: "163px",
					left: "140px"
				});
				$("#query_form").jqDrag(".dragHandle");
				$("#query_form").fadeIn("normal");
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
		$("#instructions").text("Choose an operation and fill in the parameters of your query then click run filter to see a new reuslts page with your filtered results.");
		return false;
	});
	
	$("#filter_link").click(function(){;
		if($(this).text() == "Add Step"){
			$("#filter_div").fadeIn("normal");
			$(this).text("Cancel [X]");
	}else{
			$("#filter_div").fadeOut("normal");
			$("#query_selection").show();
			$("#query_form").hide();
			$(this).text("Add Step");
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
	$("#instructions").text("Choose a query to use as a filter from the list below.  The individual queries will expad when you mouse over the categories");
}
