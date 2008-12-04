/**************************************************
Strategy Object and Functions
****************************************************/

function Strategy(frontId, backId, isDisplay){
	this.frontId = frontId;
	this.backId = backId;
	this.isDisplay = false;
}
Strategy.prototype.Steps = new Array();
Strategy.prototype.initSteps = function(steps){
	var f_index = 0;
	var arr = new Array();
	$(steps).each(function(){
		if($(this).attr("isboolean") == "false"){
			var bbid = "";
			if(this.parentNode.nodeName == "step")
				bbid = $(this).parent().attr("id");
			var s = new Step(f_index, $(this).attr("id"), bbid, "", $(this).attr("answerId"));
			arr.push(s);
			f_index++;
		}
	});
	this.Steps = arr;
}

/****************************************************
Step Object and Functions
****************************************************/

function Step(frontId, back_step_Id, back_boolean_Id, child_Strat_Id, answerId){
	this.frontId = frontId;
	this.back_step_Id = back_step_Id;
	this.back_boolean_Id = back_boolean_Id;
	this.child_Strat_Id = null;
	this.answerId = answerId;
}
Step.prototype.isboolean = false;
Step.prototype.isSelected = false;

/****************************************************
Utility Functions
*****************************************************/
	
function getStep(strat,id){
	for(i=0;i<strats.length;i++){
		if(strats[i].frontId == strat){
			for(j=0;j<strats[i].Steps.length;j++){
				if(strats[i].Steps[j].frontId == id)
					return strats[i].Steps[j];
			}
		}
	}
	return false;
}
	
function getStrategy(id){
	for(i=0;i<strats.length;i++){
		if(strats[i].frontId == id)
			return strats[i];
	}
	return false;
}

function getDataType(ele){
	var s = "";
	if(parseInt($(ele).attr("results")) > 1)
		s = "s"
	var cl = $(ele).attr("dataType");
	if(cl == "GeneRecordClasses.GeneRecordClass")
		return "Gene" + s;
}
