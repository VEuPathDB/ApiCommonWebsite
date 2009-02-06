var selected = new Array();
var currentPanel;

function selectAllHist() {
	$("div.history_panel").each(function(){
		var display = $(this).css("display");
		if (display != 'none'){
			$("input:checkbox", this).attr("checked", "yes");
		}
	});
	updateSelectedList();
}

function selectNoneHist() {
	$("div.history_panel input:checkbox").removeAttr("checked");
	selected = new Array();
}

function displayHist(type) {
	$("#selected_type").removeAttr("id");
	$(".history_panel").hide();
	selectNoneHist();
	$("#history_tabs li").each(function() {
		var id = $("a", this).attr("id");
		if (id == 'tab_' + type) {
			if (!currentPanel || currentPanel != type) currentPanel = id;
			$(this).attr("id", "selected_type");
		}
	});
	$("#panel_" + type).show();
}


function updateSelectedList() {
	selected = new Array();
	$("div.history_panel input:checkbox").each(function (i) {
		if ($(this).attr("checked")) {
			selected.push($(this).attr("id"));
		}
	});
}
		
function deleteHistories(url) {
	// make sure something is selected.
	if (selected.length == 0) {
		return false;
	}
	// else delete and replace page sections that have changed
	var agree=confirm("Are you sure you want to delete the selected histories?");
 	if (agree) {
		url = url + selected.join("&wdk_history_id=");
		$.ajax({
			url: url,
			dataType: "html",
			success: function(data) {
				$("div#mysearch").html($("div#mysearch",data).html());
				$("div.innertube").html($("div.innertube",data).html());
				if ($("#" + currentPanel).length == 0) {
					var type = $("#history_tabs a:first").attr("id").substr(4);
					displayHist(type);
				} else
					$("#" + currentPanel).show();
			},
			error: function(data, msg, e) {
				alert("ERROR \n " + msg + "\n" + e);
			}
		});
	}	
}
