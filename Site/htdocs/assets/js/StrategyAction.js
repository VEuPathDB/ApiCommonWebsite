
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

function saveStrategy(stratId){
	var saveForm = $("div#save_strat_div_" + stratId);
	var name = $("input[name='name']",saveForm).attr("value");
	var strategy = $("input[name='strategy']",saveForm).attr("value");
	var url="renameStrategy.do?strategy=";
	url = url + strategy + "&name=" + name;
	$.ajax({
		url: url,
		dataType: "html",
		success: function(data){
			// reload strategy panel by stratId
			var strat_id_span = $("div#diagram_" + strategy + " #strategy_name span#strategy_id_span");
			var small_links = $("div#diagram_" + strategy + " #strategy_name span.strategy_small_text");
			$("div#diagram_" + strategy + " #strategy_name").text(name);
			$("div#diagram_" + strategy + " #strategy_name").append(strat_id_span);
			$("div#diagram_" + strategy + " #strategy_name").append(small_links);
			saveForm.css("display","none");
			update_hist = true;
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
/*	if(proto.indexOf("_") == -1){
		var new_dia = $(".diagram",data);
		if($("#diagram_" + proto).length != 0){
			$("#diagram_" + proto).html(new_dia.html());
		}else{
			$("#Strategies").prepend(document.createElement("br"));
			$("#Strategies").prepend(new_dia);
		}
		if(needFilter){
			var new_filter = $(".filter_link_div",data);
			$("#filter_link_div_" + proto).remove();
			$("#Strategies").append(new_filter);
		}
	}else{*/
		var parts = proto.split("_");
		Refresh(parts[0]);
	//}
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
		var id_index_Map = {subid:"", subindex:""};
		$(subStrategies).each(function(){
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
/*
function refreshStrategy(stratId, newStrategy){
	var newStratId = stratId;
	var arr = stratId.split("_");
	if (newStrategy != arr[0]) {
		arr[0] = newStrategy;
		newStratId = arr.join("_");
	}
	var url="showStrategy.do?strategy=" + newStratId;
	$.ajax({
		url: url,
		async: false,
		dataType: "html",
		success: function(data){
			$("div#diagram_" + stratId).html($(".diagram", data).html());
		},
		error: function(data, msg, e){
			alert("ERROR \n "+ msg + "\n" + e);
		}
	});
}
*/


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
		dataType:"html",
		data: d,
		beforeSend: function(){
			showLoading(proto.split("_")[0]);
		},
		success: function(data){
			InsertNewStrategy(proto);
			var new_dia_id = $(".diagram",data).attr("id");
			$("#" + new_dia_id + " div.venn:last span.resultCount a").click();
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
