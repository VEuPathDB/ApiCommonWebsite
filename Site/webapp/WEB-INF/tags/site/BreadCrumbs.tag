

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bean" uri="http://jakarta.apache.org/struts/tags-bean" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>


<%@ attribute name="history"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="false"
              description="history object for this question"
%>

<%@ attribute name="wdkAnswer"
	      type="org.gusdb.wdk.model.jspwrap.AnswerValueBean"
              required="false"
              description="Answer object for this question"
%>

<%@ attribute name="model"
	      type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
              required="false"
              description="Wdk Model Object for this site"
%>

<%@ attribute name="recordClass"
	      type="org.gusdb.wdk.model.jspwrap.RecordClassBean"
              required="false"
              description="RecordClass Object for the Answer"
%>

<%@ attribute name="strategy"
	      type="org.gusdb.wdk.model.jspwrap.StrategyBean"
              required="false"
              description="Protocol from the SummaryPage"
%>

<!--<link rel="stylesheet" type="text/css" href="/assets/css/Strategy.css" />
<link rel="StyleSheet" href="/assets/css/filter_menu.css" type="text/css"/>-->
<c:set var="stepNumber" value="0" />
<div class="chain_background" id="bread_crumb_div">
	<div id="diagram">
		<div id="strategy_name">Strategy&nbsp;<span id="strategy_id_span">${strategy.strategyId}</span><span class="strategy_small_text"><br>save as<br>export</span></div>
		<c:set var="steps" value="${strategy.allSteps}" />
		<c:forEach items="${steps}" var="step">
			<site:Step step="${step}" strategy="${strategy}" stepNum="${stepNumber}"/>
			<c:set var="stepNumber" value="${stepNumber+1}" />
		</c:forEach>
	</div>
</div>




