var selected = [];

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
	if(!$("div.panel_" + type).hasClass("enabled")) {
		$("li#selected_type").removeAttr("id");
		$("div.history_panel.enabled").removeClass("enabled");
		selectNoneHist();
		$("a#tab_" + type).parent().attr("id", "selected_type");
		$("div.panel_" + type).addClass("enabled");
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
		
function downloadStep(stepId) {
	var url = "downloadStep.do?step_id=" + stepId;
	window.location = url;
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
				$("#search_history").html($("#search_history", data).html());
				$("#mysearch").html($("#mysearch", data).html());
			},
			error: function(data, msg, e) {
				alert("ERROR \n " + msg + "\n" + e);
			}
		});
	}	
}


//FOLLOWING TAKEN FROM OLD CUSTOMQUERYHISTORY

var currentStrategyId = 0;

function enableRename(histId, name) {
   // close the previous one
   disableRename();
   
   currentStrategyId = histId;
   var button = document.getElementById('activate_' + histId);
   button.style.display = 'none';
   var text = document.getElementById('text_' + histId);
   text.style.display = 'none';
   var nameBox = document.getElementById('name_' + histId);
   nameBox.innerHTML = "<input name='strategy' type='hidden' value='" + histId + "'>"
                  + "<input id='name' name='name' type='text' size='42' maxLength='2000' value='" + name + "' style='margin-right:4px;'>" 
   nameBox.style.display='block';
   var input = document.getElementById('input_' + histId);
   input.innerHTML = "<input type='submit' value='Update'>"
                   + "<input type='reset' value='Cancel' onclick='disableRename()'>";
   input.style.display='block';
   nameBox = document.getElementById('name');
   nameBox.select();
   nameBox.focus();
}

function disableRename() {
   if (currentStrategyId && currentStrategyId != '0') {
      var button = document.getElementById('activate_' + currentStrategyId);
      button.style.display = 'block';
      var name = document.getElementById('name_' + currentStrategyId);
      name.innerText = '';
      name.style.display = 'none';
      var input = document.getElementById('input_' + currentStrategyId);
      input.innerText = '';
      input.style.display = 'none';
      var text = document.getElementById('text_' + currentStrategyId);
      text.style.display = 'block';
      currentStrategyId = 0;
   }
}

function toggleEye(ele, stratId) {
	var url = "";
	var td = $(ele).parent();
	if (td.hasClass("strat_inactive")){
		openStrategy(stratId);
	}else{
		closeStrategy(stratId);
	}
}
