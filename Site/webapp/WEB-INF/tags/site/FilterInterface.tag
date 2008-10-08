
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bean" uri="http://jakarta.apache.org/struts/tags-bean" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<%@ attribute name="model"
	      type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
              required="false"
              description="Wdk Model Object for this site"
%>

<%@ attribute name="recordClass"
	          required="false"
              description="RecordClass Object for the Answer"
%>

<%@ attribute name="strategy"
	      type="org.gusdb.wdk.model.jspwrap.StrategyBean"
              required="false"
              description="strategy for this result"
%>
<span id="proto" style="display: none">${strategy.strategyId}</span>
<span id="last_step_id" style="display:none">${strategy.latestStep.stepId}</span>
<c:set var="catMap" value="${model.questionsByCategory}" />
<c:set var="recClass" value="${recordClass}" />
<c:set var="qSetName" value="none" />
<c:set var="qSets" value="${model.questionSetsMap}" />
<c:set var="qSet" value="${qSets[qSetName]}" />
<c:set var="qByCat" value="${qSet.questionsByCategory}" />

<div id="query_form" class="jqDnR">
<span class="dragHandle"><h1>Add&nbsp;Step</h1><a id='close_filter_query' href='javascript:closeAll()'><img src='/assets/images/Close-X-box.png' alt='Close'/></a></span>
<!--<div id="filter_div">-->

<div id="query_selection">
<ul class="top_nav">

<c:set var="qByCat" value="${catMap[recordClass]}" />
<c:forEach items="${qByCat}" var="cat">
	<li><a class="category" href="javascript:void(0)">${cat.key}</a>
	<ul>
	<c:forEach items="${cat.value}" var="q">
		<li><a href="javascript:getQueryForm('showQuestion.do?questionFullName=${q.fullName}')">${q.displayName}</a></li>
	</c:forEach>
	</ul>
</c:forEach>

</ul>
</div><!-- End of Query Selection Div -->
<!--</div> End of Filter div -->
<!--<div id="query_form" class="jqDnR">-->
</div><!-- End of Query Form Div -->

