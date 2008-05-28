$("#diagram").ready(function(){
	$(".crumb_name").mouseover(function(){
		var detail = $(this).parent().siblings(".crumb_details");
		detail.show();
	}); 
	
	$(".crumb_name").mouseout(function(){
		var detail = $(this).parent().siblings(".crumb_details");
		detail.hide();		
	});
});