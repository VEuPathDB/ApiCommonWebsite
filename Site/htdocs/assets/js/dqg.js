var dqg_speed = "fast";
var sidebar_speed = "fast";
$(document).ready(function(){
	//hide all of the sub list
	$("div.sub_list").hide();
//	$("ul.heading_list li img").hide();
	$("ul.heading_list li a.heading").click(function(){
		var sublist = $(this).parents("li").find("div.sub_list");
		sublist.slideToggle(dqg_speed);
		return false;
	});
	
	$("p.small a").click(function(){
		var val = $(this).attr("href");
		var headList = $(this).parent().siblings("ul.heading_list");
		if(val == "true"){
			$("div.sub_list", headList).slideDown(dqg_speed);
		}else{
			$("div.sub_list", headList).slideUp(dqg_speed);
		}
		return false;
	});
	
	var top_div = $("div#menu_lefttop");
	$("div", top_div).hide();
	var op = $("div:first", top_div);
	$("div:first", top_div).show();
	$("a.heading", top_div).click(function(){
		var testOp = op.prev("a").text();
		if(op.prev("a").text() != $(this).text()){
			op.hide(sidebar_speed);
			op = $(this).next("div");
			$(this).next("div").show(sidebar_speed);
		}
		return false;
	});
});

