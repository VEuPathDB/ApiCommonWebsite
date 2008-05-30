

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>

<%@ attribute name="step"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step to be displayed by this tag"
%>
<%@ attribute name="protocol"
	      type="org.gusdb.wdk.model.jspwrap.ProtocolBean"
              required="true"
              description="Protocol containing this Step"
%>
<%@ attribute name="stepNum"
	      type="java.lang.String"
              required="true"
              description="Number of this step in the protocol"
%>
<c:set var="left_offset" value="0" />
<c:choose>
	<c:when test="${stepNum == 1}">
		<c:set var="left_offset" value="18" />
	</c:when>
	<c:otherwise>
		<c:set var="left_offset" value="${((stepNum - 1) * 11.65) + 18}" />
	</c:otherwise>
</c:choose>

<c:set var="stepName" value="${step.customName}" />
<c:if test="${fn:length(stepName) > 15}">
	<c:set var="stepName" value="${fn:substring(stepName,0,12)}..."/>
</c:if>

<c:choose>
	<c:when test="${step.isFirstStep}">
		<div id="step_${stepNum}" class="row2 col1 size1 arrowgrey">
			<h3>
				<a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNum}">${stepName}</a><br>
			</h3>
			<site:StepDetail step="${step}"/>
			<span class="resultCount">Results:&nbsp;${step.filterResultSize}</span>			
			<c:if test="${step.nextStep != null}">
				<ul>
					<li class='right width1'><span>&nbsp;</span></li>
				</ul>
			</c:if>
		</div>
		<span class="stepNumber"style="left:3em;">Step&nbsp;${stepNum + 1}</span>
	</c:when>
	<c:otherwise>
		<div id="step_${stepNum}_sub" class='row1 size1 arrowgrey' style='left:${left_offset - 8}em'>
			<h3>
				<a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNum}&subquery=true">${stepName}</a><br>
			</h3>
			<site:StepDetail step="${step}"/>
			<span class="resultCount">Results:&nbsp;${step.subQueryResultSize}</span>
			<ul>
				<li class='right-down width2'><span>&nbsp;</span></li>
			</ul>
		</div>
		<div id="step_${stepNum}" class="row2 size2 operation ${step.operation}" style="left:${left_offset}em; top: 5em; border: none">
			<a class="operation" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNum}"><img src="/assets/images/transparent1.gif"/></a><br>
			<span class="resultCount">Results:&nbsp;${step.filterResultSize}</span>
			<c:if test="${step.nextStep != null}">
				<ul>
					<li class='right width15'><span>&nbsp;</span></li>
				</ul>
			</c:if>
		</div>
		<span class="stepNumber" style="left:${left_offset}em">Step&nbsp;${stepNum + 1}</span>
	</c:otherwise>
</c:choose>
