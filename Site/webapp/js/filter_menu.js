
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
				
				var quesForm = $("form",data);
				quesForm.prepend("<input type='radio' name='myProp(booleanExpression)' value='" + historyId + " AND'/>&nbsp;AND&nbsp;<input type='radio' name='myProp(booleanExpression)' value='" + historyId + " OR'>&nbsp;OR&nbsp;<input type='radio' name='myProp(booleanExpression)' value='" + historyId + " NOT'>&nbsp;NOT&nbsp<br>");
				var action = quesForm.attr("action").replace(/processQuestion/,"processFilter");
				quesForm.attr("action",action);
				$("#query_form").html(quesForm);
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
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
