

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>

<%@ attribute name="step"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step to be displayed by this tag"
%>
<%@ attribute name="strategy"
	      type="org.gusdb.wdk.model.jspwrap.StrategyBean"
              required="true"
              description="Protocol containing this Step"
%>
<%@ attribute name="stepNum"
	      type="java.lang.String"
              required="true"
              description="Number of this step in the strategy"
%>

<c:set var="type" value="None" />
<c:set var="step_dataType" value="${step.dataType}" />
<c:choose>
	<c:when test="${step_dataType == 'GeneRecordClasses.GeneRecordClass'}">
		<c:set var="type" value="Genes" />
	</c:when>
	<c:when test="${step_dataType == 'SequenceRecordClasses.SequenceRecordClass'}">
		<c:set var="type" value="Seq" />
	</c:when>
	<c:when test="${step_dataType == 'EstRecordClasses.EstRecordClass'}">
		<c:set var="type" value="EST" />
	</c:when>
	<c:when test="${step_dataType == 'OrfRecordClasses.OrfRecordClass'}">
		<c:set var="type" value="ORF" />
	</c:when>
	<c:when test="${step_dataType == 'SnpRecordClasses.SnpRecordClass'}">
		<c:set var="type" value="SNP" />
	</c:when>
	<c:when test="${step_dataType == 'AssemblyRecordClasses.AssemblyRecordClass'}">
		<c:set var="type" value="Assm" />
	</c:when>
	<c:when test="${step_dataType == 'IsolateRecordClasses.IsolateRecordClass'}">
		<c:set var="type" value="Iso" />
	</c:when>	
</c:choose>


<c:set var="left_offset" value="0" />
<c:choose>
	<c:when test="${stepNum == 1}">
		<!--<c:set var="left_offset" value="18" />-->
		<c:set var="left_offset" value="11.3" />
	</c:when>
	<c:otherwise>
		<!--<c:set var="left_offset" value="${((stepNum - 1) * 11.65) + 18}" />-->
		<c:set var="left_offset" value="${((stepNum) * 11.3) - (stepNum - 1)}" />
	</c:otherwise>
</c:choose>



<c:choose>
	<c:when test="${step.isFirstStep}">
	
	<c:set var="stepName" value="${step.shortDisplayName}" />
	<c:if test="${fn:length(stepName) > 15}">
		<c:set var="stepName" value="${fn:substring(stepName,0,12)}..."/>
	</c:if>
	
		<div id="step_${stepNum}" class="box row2 col1 size1 arrowgrey">
			<h3>
				<a id="stepId_${step.stepId}" class="crumb_name" onclick="showDetails(this)" href="javascript:void(0)">${stepName}</a>
				<site:StepDetail step="${step}" strategyNum="${strategy.strategyId}" stepNum="${stepNum}"/>
			</h3>
			<span class="resultCount"><a class="results_link" href="javascript:void(0)" onclick="NewResults(this,'showSummary.do?strategy=${strategy.strategyId}&step=${stepNum}&resultsOnly=true')"> ${step.resultSize}&nbsp;${type}</a></span>			
			<c:if test="${step.nextStep != null}">
				<ul>
					<li><img class="rightarrow1" src="/assets/images/arrow_chain_right3.png"></li>
				</ul>
			</c:if>
		</div>
		<span class="stepNumber"style="left:3em;">Step&nbsp;${stepNum + 1}</span>
	</c:when>
	<c:otherwise>
	
	<c:set var="stepName" value="${step.childStep.shortDisplayName}" />
	<c:if test="${fn:length(stepName) > 15}">
		<c:set var="stepName" value="${fn:substring(stepName,0,12)}..."/>
	</c:if>
	
		<div id="step_${stepNum}_sub" class='box row1 size1 arrowgrey' style='left:${left_offset}em'>
			<h3>
				<a id="stepId_${step.stepId}" class="crumb_name" onclick="showDetails(this)" href="javascript:void(0)">${stepName}</a>
				<site:StepDetail step="${step}" strategyNum="${strategy.strategyId}" stepNum="${stepNum}"/>
			</h3>
			<span class="resultCount"><a class="results_link" href="javascript:void(0)" onclick="NewResults(this,'showSummary.do?strategy=${strategy.strategyId}&step=${stepNum}&subquery=true&resultsOnly=true')"> ${step.childStep.resultSize}&nbsp;${type}</a></span>
			<ul>
				<li><img class="downarrow" src="/assets/images/arrow_chain_down2.png"</li>
			</ul>
		</div>
		<div id="step_${stepNum}" class="venn row2 size2 operation ${step.operation}" style="left:${left_offset}em; top: 4.5em;">
			<a class="operation" onclick="NewResults(this,'showSummary.do?strategy=${strategy.strategyId}&step=${stepNum}&resultsOnly=true')" href="javascript:void(0)"><img src="/assets/images/transparent1.gif"/></a><br>
			<span class="resultCount"><a class="operation" onclick="NewResults(this,'showSummary.do?strategy=${strategy.strategyId}&step=${stepNum}&resultsOnly=true')" href="javascript:void(0)">${step.resultSize}&nbsp;${type}</a></span>
			<c:if test="${step.nextStep != null}">
				<ul>
					<li><img class="rightarrow2" src="/assets/images/arrow_chain_right4.png"></li>
				</ul>
			</c:if>
		</div>
		<span class="stepNumber" style="left:${left_offset+2.7}em">Step&nbsp;${stepNum + 1}</span>
	</c:otherwise>
</c:choose>
