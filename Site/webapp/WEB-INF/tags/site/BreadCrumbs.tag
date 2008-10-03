

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bean" uri="http://jakarta.apache.org/struts/tags-bean" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>

<%@ attribute name="strategy"
	      type="org.gusdb.wdk.model.jspwrap.StrategyBean"
              required="false"
              description="Protocol from the SummaryPage"
%>

<!--<link rel="stylesheet" type="text/css" href="/assets/css/Strategy.css" />
<link rel="StyleSheet" href="/assets/css/filter_menu.css" type="text/css"/>-->
<c:set var="stepNumber" value="0" />
<!--<div class="chain_background" id="bread_crumb_div">-->
	<div class="diagram" id="diagram_${strategy.strategyId}">
		<span class="closeStrategy"><a href="javascript:void(0)" onclick="closeStrategy(${strategy.strategyId})"><img src="/assets/images/Close-X.png" alt="click here to remove ${strategy.name} from the list"/></a></span>
		<div id="strategy_name">${strategy.name}<span id="strategy_id_span" style="display:none">${strategy.strategyId}</span><span class="strategy_small_text"><br>save as<br>export</span></div>
		<c:set var="steps" value="${strategy.allSteps}" />
		<c:forEach items="${steps}" var="step">
			<site:Step step="${step}" strategy="${strategy}" stepNum="${stepNumber}"/>
			<c:set var="stepNumber" value="${stepNumber+1}" />
		</c:forEach>
		<a class="filter_link redbutton" onclick="this.blur()" href="javascript:openFilter('${strategy.strategyId}:')" id="filter_link"><span>Add Step</span></a>
	</div>
	<div class="filter_link_div" id="filter_link_div_${strategy.strategyId}">
		<site:FilterInterface model="${applicationScope.wdkModel}" recordClass="${strategy.latestStep.dataType}" strategy="${strategy}"/>
	</div>
<!--</div>-->




