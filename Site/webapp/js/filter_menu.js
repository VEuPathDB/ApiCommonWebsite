
$(document).ready(function(){
	$("#filter_div").hide();
//	$("#bottom_filter_line").hide();
	
	$(".top_nav ul li a").click(function(){
		
		var url = $(this).attr("href");
		//$.get(url, function(data){
		//		$("#query_form").html(data);
		//	}
		//);
		$.ajax({
			url: url,
			dataType:"html",
			//beforeSend: function(req){
			//	alert("AJAX CALL BEING MADE!!!");
			//},
			success: function(data){
				var historyId = $("#history_id").val();
				
				var quesTitle = $("b font[size=+3]",data).parent().text().replace(/Identify Genes based on/,"");
				var quesForm = $("form",data);

				$("input[value=Get Answer]",quesForm).val("Run Filter");

				quesForm.prepend("<span class='radio'><b>Operator</b><input type='radio' name='myProp(booleanExpression)' value='" + historyId + " AND' checked='checked'/>&nbsp;AND&nbsp;<input type='radio' name='myProp(booleanExpression)' value='" + historyId + " OR'>&nbsp;OR&nbsp;<input type='radio' name='myProp(booleanExpression)' value='" + historyId + " NOT'>&nbsp;NOT&nbsp</span><br>");

				var action = quesForm.attr("action").replace(/processQuestion/,"processFilter");

				quesForm.prepend("<span id='question_title'>" + quesTitle + " Filter</span><br>");

				quesForm.attr("action",action);
				$("#query_form").html(quesForm);
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
		$("#instructions").text("Choose an operation and fill in the parameters of your query then click run filter to see a new reuslts page with your filtered results.");
		return false;
	});
	
	$("#filter_link").click(function(){
		$("#filter_div").slideToggle("fast");
		//$("#bottom_filter_line").slideToggle("slow");
		if($(this).text() == "Create Filter")
			$(this).text("Cancel [X]");
		else
			$(this).text("Create Filter");
	});
	
});

