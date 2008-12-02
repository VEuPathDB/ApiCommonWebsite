var strats = new Array();
var xmldoc = null

$(document).ready(function(){
	$("#Strategies div[id^='diagram_']").each(function(){
		$.ajax({
			url: "showStrategy.do?strategy=" + this.id.substring(8),
			dataType: "XML",
			success: function(data){
				loadModel(data);
			}
		});
	});
});

function loadModel(data){
	var index = 0;
	$("strategy",data).each(function(){
		xmldoc = data;
		strat = new Strategy(index, $(this).attr("id"), false);
		strat.initSteps($("step",this));
		strats.push(strat);
		index++;
	});
}

function displayModel(strat_id){
	if(strats){
		var strat = null;
		if(strat_id < strats.length)
			strat = strats[strat_id];
		var div_strat = document.createElement("div");
		$(div_strat).attr("id","diagram_" + strat.frontId).addClass("diagram");
		$(div_strat).append(createStrategyName($("strategy#" + strat.backId,xmldoc), strat));
		for(var j=0;j<strat.Steps.length;j++){
			last = false;
			if(j == strat.Steps.length - 1) 
				last = true;
			if(strat.Steps[j].back_boolean_Id == ""){
				var xml_step = $("strategy#" + strat.backId + " step#" + strat.Steps[j].back_step_Id, xmldoc);
				st = createStep(xml_step, strat.Steps[j], last);
				$(div_strat).append(st[0]);
				$(div_strat).append(st[1]);
			}else {
				var xml_step_boolean = $("strategy#" + strat.backId + " step#" + strat.Steps[j].back_boolean_Id, xmldoc);
				var xml_step_operand = $("strategy#" + strat.backId + " step#" + strat.Steps[j].back_step_Id, xmldoc);
				$(div_strat).append(createStep(xml_step_operand, strat.Steps[j], last)[0]);
				strat.Steps[j].isboolean = true;
				st = createStep(xml_step_boolean, strat.Steps[j], last);
				$(div_strat).append(st[0]);
				$(div_strat).append(st[1]);
				strat.Steps[j].isboolean = false;
			}
		}
		
		buttonleft = offset(strat.Steps.length);
		button = document.createElement('a');
		$(button).attr("id","filter_link").attr("href","javascript:openFilter('" + strats.frontId + ":')").attr("onclick","this.blur()").addClass("filter_link redbutton");
		$(button).html("<span>Add Step</span>");
		$(button).css({ position: "absolute",
						left: buttonleft + "em",
						top: "4.5em"});
		$(div_strat).append(button);
	return div_strat;
	}
	return null;
}

function offset(index){
		return (index * 11.3) - (index - 1);
}

function createStep(ele, step,isLast){
	var name = $(ele).attr("name");
	var shortName = $(ele).attr("shortName");
	var collapsible = $(ele).attr("isCollapsed");
	var resultSize = $(ele).attr("results");
	var operation = $(ele).attr("operation");
	var dataType = getDataType(ele);
	var id = step.frontId;
	var cl ="";
	var inner = "";
	if(step.back_boolean_Id == ""){
		div_id = "step_" + id;
		left = -1;
		cl = "box venn row2 col1 size1 arrowgrey";
		inner = ""+
			"		<h3>"+
			"			<a id='stepId_" + id + "' class='crumb_name' onclick='showDetails(this)' href='javascript:void(0)'>"+
							shortName +
			"				<span class='collapsible' style='display: none;'>" + collapsible + "</span>"+
			"			</a>"+
			"			<div class='crumb_details'></div>"+
			"		</h3>"+
			"		<span class='resultCount'><a class='results_link' href='javascript:void(0)' onclick='NewResults()'> " + resultSize + "&nbsp;" + dataType + "</a></span>";
		if(!isLast){
			inner = inner + 
			"		<ul>"+
			"			<li><img class='rightarrow1' src='/assets/images/arrow_chain_right3.png' alt='input into'></li>"+
			"		</ul>";
		}
		stepNumber = document.createElement('span');
		$(stepNumber).addClass('stepNumber').css({ left: "3.7em"}).text("Step " + (id + 1));
	}else if(step.isboolean){
		div_id = "step_" + id;
		left = offset(id);	
		cl = "venn row2 size2 operation " + operation;
		inner = ""+
			"			<a class='operation' onclick='NewResults()' href='javascript:void(0)'>"+
			"				<img src='/assets/images/transparent1.gif'>"+
			"			</a>"+
			"			<span class='resultCount'>"+
			"				<a class='operation' onclick='NewResults()' href='javascript:void(0)'>" + resultSize + "&nbsp;" + dataType + "</a>"+
			"			</span>";
		if(!isLast){
			inner = inner + 
			"			<ul>"+
			"				<li><img class='rightarrow2' src='/assets/images/arrow_chain_right4.png' alt='input into'></li>"+
			"			</ul>";
		}
		stepNumber = document.createElement('span');
		$(stepNumber).addClass('stepNumber').css({ left: (left + 2.7) + "em"}).text("Step " + (id + 1));
	}else{
		div_id = "step_" + id + "_sub";
		left = offset(id);
		cl = "box row1 size1 arrowgrey";
		inner = ""+
			"		<h3>"+
			"			<a id='stepId_" + id + "' class='crumb_name' onclick='showDetails(this)' href='javascript:void(0)'>"+
							shortName +
			"				<span class='collapsible' style='display: none;'>" + collapsible + "</span>"+
			"			</a>"+
			"			<div class='crumb_details'></div>"+
			"		</h3>"+
			"		<span class='resultCount'><a class='results_link' href='javascript:void(0)' onclick='NewResults()'> " + resultSize + "&nbsp;" + dataType + "</a></span>"+
			"		<ul>"+
			"			<li><img class='downarrow' src='/assets/images/arrow_chain_down2.png' alt='equals'></li>"+
			"		</ul>";
		stepNumber = null;
	}
	var divs = new Array();
	var div_s = document.createElement("div");
	if(left != -1){
		$(div_s).attr("id", div_id).addClass(cl).html(inner);
		$(div_s).css({ left: left + "em"});
	}else{
		$(div_s).attr("id", div_id).addClass(cl).html(inner);
	}
	divs.push(div_s);
	divs.push(stepNumber);
	return divs;
}

function createParameters(param){
	
}

function createStrategyName(ele, strat){
	var id = strat.backId;
	var name = $(ele).attr("name");
	
	var div_sn = document.createElement("div");
	$(div_sn).attr("id","strategy_name");
	$(div_sn).html(name + "<span id='strategy_id_span' style='display: none;'>" + id + "</span>" +
	"<span class='strategy_small_text'>" +
	"<br/>" +
	"<a class='save_strat_link' href='javascript:void(0)' onclick='showSaveForm('" + id + "')'>save as</a>" +
	"<div id='save_strat_div_" + id + "' class='modal_div save_strat'>" +
	"<span class='dragHandle'>" +
	"<div class='modal_name'>"+ 
	"</div>"+
	"<a class='close_window' href='javascript:closeModal()'>"+
	"</a>"+
	"</span>"+
	"<form onsubmit='return validateSaveForm(this);' action='javascript:saveStrategy('" + id + "', true)'>"+
	"<input type='hidden' value='" + id + "' name='strategy'/>"+
	"<input type='text' value='' name='name'/>"+
	"<input type='submit' value='Save'/>"+
	"</form>"+
	"</div>"+
	"<br/>"+
	"<a href='javascript:showExportLink('" + id + "')'>export</a>"+
	"<div id='export_link_div_" + id + "' class='modal_div export_link'>"+
	"</div>"+
	"</span>");
	return div_sn;
}

///////// ^^^^^^^ NEW CODE ^^^^^   ///////////////////////////////// vvvvvv OLD CODE vvvvvvvv ///////////////////////////////////////////



var isInsert = "";
function openStrategy(stratId){
	var url = "showStrategy.do?strategy=" + stratId;
	$.ajax({
		url: url,
		datatype:"html",
		success: function(data){
			InsertNewStrategy(stratId, data, true);
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	$("#eye_" + stratId).removeClass("strat_inactive").addClass("strat_active");
}

function closeStrategy(stratId){
	if(stratId.indexOf("_") == -1){
		var url = "closeStrategy.do?strategy=" + stratId;
		$.ajax({
			url: url,
			dataType:"html",
			success: function(data){
				$("#diagram_" + stratId).hide("slow").remove();
			},
			error: function(data, msg, e){
				alert("ERROR \n "+ msg + "\n" + e);
			}
		});
		$("#eye_" + stratId).removeClass("strat_active").addClass("strat_inactive");
	} else {
		var parts = stratId.split("_");
		$("#diagram_" + stratId).hide("slow").remove();
		var ps_height = $("#diagram_" + parts[0]).css("height");
		ps_height = ps_height.substring(0, ps_height.indexOf("px"));
		ps_height = parseInt(ps_height) - 132;
		ps_height = ps_height + "px";
		$("#diagram_" + parts[0]).css({ height: ps_height });
	}
	$("#filter_link_div_" + stratId).remove();
}

function saveStrategy(stratId, checkName){
	var saveForm = $("div#save_strat_div_" + stratId);
	var name = $("input[name='name']",saveForm).attr("value");
	var strategy = $("input[name='strategy']",saveForm).attr("value");
	var url="renameStrategy.do?strategy=";
	url = url + strategy + "&name=" + name + "&checkName=" + checkName;
	$.ajax({
		url: url,
		dataType: "html",
		success: function(data){
			// reload strategy panel
			if (data) {
	                        var diagram = $("div.diagram", data);
				// save successful, we got a diagram
				$("div#diagram_" + strategy + " #strategy_name").html($("#strategy_name", diagram).html());
				saveForm.hide()
				update_hist = true;
			}
			else{
				// data == "" -> save unsuccessful -> name collision
				var overwrite = confirm("A strategy already exists with the name '" + name + ".' Do you want to overwrite the existing strategy?");
				if (overwrite) {
					saveStrategy(stratId, false);
				}
				else {
					saveForm.hide();
				}
			}
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
}

function showLoading(divId){
	var d = $("#diagram_" + divId);
	var l = document.createElement('span');
	$(l).attr("id","loadingGIF");
	var i = document.createElement('img');
	$(i).attr("src","/assets/images/loading.gif");
	$(i).attr("height","23");
	$(l).append(i);
	$(l).css({
		position: "absolute",
		left: "10px",
		top: "10px"
	});
	$(d).append(l);
}

function InsertNewStrategy(proto){
	var parts = proto.split("_");
	Refresh(parts[0]);
}

var recur_Count;
var sub_strat_ids;
var count;
function Refresh(strategyId){
	var parent_strat = $("#diagram_" + strategyId);
	var subStrategies = $("#diagram_" + strategyId + " div[id^='diagram_" + strategyId + "_']");
	sub_strat_ids = new Array();
	count = 0;
	if(subStrategies.length != 0){
		
		$(subStrategies).each(function(){
			var id_index_Map = {subid:"", subindex:""};
			id_index_Map.subid = $(this).attr("id").substring(8);
			var temp = $("#diagram_" + strategyId + " #stepId_" + id_index_Map.subid.split("_")[1]).parent().parent().attr("id");
			id_index_Map.subindex = temp.substring(5,temp.indexOf("_sub"));
			sub_strat_ids[count] = id_index_Map;
			count++;
		});
	}
	recur_Count = 0;
	recursiveRefresh(strategyId);
}

function recursiveRefresh(stratId){
	var parent_Strat = stratId;
	if(stratId.indexOf("_") != -1){
		parent_Strat = stratId.split("_")[0]
	}
	var url="showStrategy.do?strategy=" + stratId;
	$.ajax({
		url: url,
		dataType: "html",
		success: function(data){
			if($("div#diagram_" + stratId).length != 0){
				$("div#diagram_" + stratId).html($(".diagram", data).html());
				showLoading(stratId);
			}else{
				$("#loadingGIF").remove();
				var dia_id = stratId;
				if(stratId.split("_").length > 1){
					
					$("#diagram_" + parent_Strat).append($(".diagram", data).addClass("sub_diagram").css({left: "36px",width: "97%",top: "118px"
					}));
					$("#Strategies").append($(".filter_link_div", data));
				}else{
					$("#Strategies").append($(".diagram", data));
					$("#Strategies").append($(".filter_link_div", data));
				}	
			}
			if(recur_Count != count){
				var id = sub_strat_ids[recur_Count];
				recur_Count++;
				var new_step_id = $("#diagram_" + parent_Strat + " #step_" + id.subindex + "_sub h3 a[id^='stepId_']").attr("id").substring("7");
				var parts = id.subid.split("_");
				parts[1] = new_step_id;
				var sub_diagram_id = parts.join("_");
				recursiveRefresh(sub_diagram_id);
			}else{
				$("#diagram_" + stratId.split("_")[0] + " span#loadingGIF").remove();
			}
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
}

function AddStepToStrategy(proto, act){
	var url = act;	
	var d = parseInputs();
	if(proto.indexOf("_") != -1){
		var strat_step = proto.split("_");
		var oldStep = $("#diagram_"+strat_step[0]+" #stepId_"+strat_step[1]);
		var stepIndex = $(oldStep).parent().parent().attr("id");
		stepIndex = stepIndex.substring(5,stepIndex.indexOf("_sub"));
		proto = proto+"_"+stepIndex;
	}
	
	$.ajax({
		url: url,
		type: "POST",
		dataType:"xml",
		data: d,
		beforeSend: function(){
			showLoading(proto.split("_")[0]);
		},
		success: function(data){
			loadModel(data);
			$("div#Strategies div#diagram_" + proto).remove();
			$("div#Strategies"). append(displayModel(0));
			$("#diagram_0 div.venn:last span.resultCount a").addClass("selected");//click();
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	update_hist = true;
	openFilter(isInsert);
}

function EditStep(proto, url, step_number){
	$("#query_form").hide("fast");
	var d = parseInputs();
		$.ajax({
		url: url,
		type: "POST",
		dataType:"html",
		data: d,
		beforeSend: function(obj){
				showLoading(proto.split("_")[0]);
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
			InsertNewStrategy(proto);
			$("#diagram_" + proto.split("_")[0] + "span#loadingGIF").remove();
		    $("#"+selected_div+" span.resultCount a").click();
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	openFilter(proto+":");
	update_hist = true;
}

function DeleteStep(ele,url){
	$(ele).parent().parent().hide();
	var deleted_step_id = url.substring(url.indexOf("step=") + 5);
	var parentStratNum = parseUrl("strategy",url);
	$.ajax({
		url: url,
		type: "GET",
		dataType:"html",
		beforeSend: function(obj){
				showLoading(parentStratNum);
			},
		success: function(data){
			var diagramId = $(".diagram",data).attr("id");
			var proto = diagramId.split("_")[1];
			var diagram_divs = $("#" + diagramId + " div");
			var selected_div = "";
			for(i=0; i < diagram_divs.length;i++){
				var b = $(diagram_divs[i]);
				if($(diagram_divs[i]).hasClass("selectedarrow") || $(diagram_divs[i]).hasClass("selected")){
					selected_div = $(diagram_divs[i]).attr("id");
				}
			}
			InsertNewStrategy(proto);
			$("#diagram_" + parentStratNum + "span#loadingGIF").remove();
			
		    if(selected_div == "step_"+deleted_step_id || selected_div == "step_"+deleted_step_id+"_sub"){
					$("#"+diagramId+" div.venn:last span.resultCount a").click();
			}else{
				var selected_id = parseInt(selected_div.substring(5,6)) - 1;
				selected_div = selected_div.substring(0,5) + selected_id + selected_div.substring(6);
		    	$("#"+diagramId+" #"+selected_div+" span.resultCount a").click();
			}
		
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
	update_hist = true;
}

function ExpandStep(url){
	var parentStratNum = parseUrl("strategy",url);
	var strat_div = $("#Strategies");
	var parent_strat = $("#diagram_" + parentStratNum);
	$.ajax({
		url: url,
		dataType: "html",
		beforeSend: function(){
			showLoading(parentStratNum);
		},
		success: function(data){
			var sub = $(".diagram",data);
			sub.addClass("sub_diagram");
			var parentStepId = sub.attr("id");
			parentStepId = parentStepId.split("_")[2];
			var parentStep = parent_strat.find("#stepId_" + parentStepId);
			if(parentStep.find(".collapsible").html() == "false"){
				var exName = sub.find("#strategy_name")[0].firstChild;
				if(exName.length > 15)
					exName = exName.nodeValue.substring(0,11) + "...";
				parentStep.text(exName);
			}
			var filter = $(".filter_link_div", data);
			var ps_height = $("#diagram_" + parentStratNum).css("height");
			ps_height = ps_height.substring(0, ps_height.indexOf("px"));
			ps_height = parseInt(ps_height) + 132;
			ps_height = ps_height + "px";
			parent_strat.css({
				height: ps_height
			});
			sub.css({
				left: "36px",
				width: "97%",
				top: "118px"
			});
			parent_strat.append(sub);
			parent_strat.children("span#loadingGIF").remove();
			strat_div.append(filter);
		},
		error: function(data, msg, e){
			alert("ERROR \n " + msg + "\n" + e);
		}
	});
	update_hist = true;
}
