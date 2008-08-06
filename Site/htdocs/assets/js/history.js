var selected = [];

$("#search_history").ready(function() {
	$("tbody[id*='steps'] tr").each(function() {
		this.style.display = "none";
	});
});

function toggleSteps(strat) {
	var img = $("img#img_" + strat);
	if (img.hasClass("plus")) {
		$("tbody#steps_" + strat + " tr").each(function() {
							this.style.display = "";
							});
	        img[0].src = "/assets/images/sqr_bullet_minus.png";
		img.removeClass("plus");
		img.addClass("minus");
	}
	else {
		$("tbody#steps_" + strat + " tr").each(function() {
							this.style.display = "none"
							});
	        img[0].src = "/assets/images/sqr_bullet_plus.png";
		img.removeClass("minus");
		img.addClass("plus");
	}
}	

function selectAllHist() {
	$("div.history_panel.enabled input:checkbox").attr("checked", "yes");
	updateSelectedList();
}

function selectNoneHist() {
	$("div.history_panel input:checkbox").removeAttr("checked");
	selected = [];
}

function displayHist(type) {
	if(!$("div#panel_" + type).hasClass("enabled")) {
		$("li#selected").removeAttr("id");
		$("div.history_panel.enabled").removeClass("enabled");
		selectNoneHist();
		$("a#tab_" + type).parent().attr("id", "selected");
		$("div#panel_" + type).addClass("enabled");
	}
}

function updateSelectedList() {
	selected = [];
	$("div.history_panel input:checkbox").each(function (i) {
		if ($(this).attr("checked")) {
			selected.push($(this).attr("id"));
		}
	});
}
		


function deleteStrategies(url) {
	// make sure something is selected.
	if (selected.length == 0) {
		return false;
	}
	// else delete and replace page sections that have changed
	var agree=confirm("Are you sure you want to delete the selected strategies?");
 	if (agree) {
		url = url + selected.join("&strategy=");
		$.ajax({
			url: url,
			dataType: "html",
			success: function(data) {
				//$("div#menubar").html($("div#menubar", data));
				//$("div#search_history").html($("div#search_history", data));
				$(this).html(data);
			},
			error: function(data, msg, e) {
				alert("ERROR \n " + msg + "\n" + e);
			}
		});
	}	
}
