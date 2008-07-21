<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="pg" uri="http://jsptags.com/tags/navigation/pager" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>

<c:set var="wdkAnswer" value="${requestScope.wdkAnswer}"/>
<c:set var="history" value="${requestScope.wdkHistory}"/>
<c:set var="model" value="${applicationScope.wdkModel}" />
<c:set var="strategy" value="${requestScope.wdkStrategy}" />
<c:set var="commandUrl">
    <c:url value="/processSummary.do?${wdk_query_string}" />
</c:set>

<c:set var="type" value="None" />
<c:choose>
	<c:when test="${wdkAnswer.recordClass.fullName == 'GeneRecordClasses.GeneRecordClass'}">
		<c:set var="type" value="Gene" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'SequenceRecordClasses.SequenceRecordClass'}">
		<c:set var="type" value="Sequence" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'EstRecordClasses.EstRecordClass'}">
		<c:set var="type" value="EST" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'OrfRecordClasses.OrfRecordClass'}">
		<c:set var="type" value="ORF" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'SnpRecordClasses.SnpRecordClass'}">
		<c:set var="type" value="SNP" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'AssemblyRecordClasses.AssemblyRecordClass'}">
		<c:set var="type" value="Assembly" />
	</c:when>
	<c:when test="${wdkAnswer.recordClass.fullName == 'IsolateRecordClasses.IsolateRecordClass'}">
		<c:set var="type" value="Isolate" />
	</c:when>	
</c:choose>

<site:home_header refer="customSummary" />
<site:menubar />

<script type="text/javascript">
<!--

function addAttr() {
    var attributeSelect = document.getElementById("addAttributes");
    var index = attributeSelect.selectedIndex;
    var attribute = attributeSelect.options[index].value;
    
    if (attribute.length == 0) return;

    var url = "${commandUrl}&command=add&attribute=" + attribute;
    window.location.href = url;
}


function resetAttr() {
    if (confirm("Are you sure you want to reset the column configuration back to the default?")) {
        var url = "${commandUrl}&command=reset";
        window.location.href = url;
    }
}
//-->
</script>

<div id="contentwrapper">
  	<div id="contentcolumn2">
		<div class="innertube">
<div class="strategy_controls"/>
<table width="100%">
<tr>
  <td width="50%">       <%--     <span id="strategy_name">  makes eh title move down..... --%>
     <h2><b>My ${type} Search Strategy and Results</b></h2>
  </td>
  <td width="50%" align="right">
     <input type="submit" value="Save" name="saveStrategy" disabled/>
     <input type="submit" value="Export" name="exportStrategy" disabled/>
     <input type="submit" value="Start&nbsp;Over" name="newStrategy" disabled/>



</td>
</tr>
<tr>
<td colspan="2" align="center">
<font size ="-2">Click on <font color="darkred"><b>Add Step</b></font> to refine your current result with an additional search. &nbsp;&nbsp;&nbsp;Mouse over a query name to <font color="darkred"><b>Edit</b></font> a query.</font>
</td>
</tr>
</table>
</div>

<input type="hidden" id="history_id" value="${history.userAnswerId}"/>
<div id="Strategies">
	<div id="loading_step_div"></div>
	<site:BreadCrumbs history="${history}" wdkAnswer="${wdkAnswer}" model="${model}" recordClass="${wdkAnswer.recordClass}" strategy="${strategy}"/>
	<hr>
</div>

<input type="hidden" id="target_step" value="${stepNumber+1}"/>

<div id="filter_link_div">
	<site:FilterInterface model="${model}" recordClass="${wdkAnswer.recordClass}" strategy="${strategy}"/>
</div>

<div id="Workspace">
<site:Results />
</div>
</div>
</div>
</div>
<site:footer />
