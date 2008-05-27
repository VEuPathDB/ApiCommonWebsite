

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
<c:set var="stepName" value="${step.customName}" />
<c:if test="${fn:length(stepName) > 15}">
	<c:set var="stepName" value="${fn:substring(stepName,0,12)}..."/>
</c:if>

<c:choose>
	<c:when test="${step.isFirstStep}">
		<div id="current" class="row2 col1 size1">
			<h3>
				<a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNum}">${stepName}</a>
			</h3>
			<c:if test="${step.nextStep != null}">
				<ul>
					<li class='right width1'><span>&nbsp;</span></li>
				</ul>
			</c:if>
		</div>
	</c:when>
	<c:otherwise>
		<div class='row1 size1' style='left:${stepNum * 18 - 8}em'>
			<h3>
				<a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNum}">${stepName}</a>
			</h3>
			<ul>
				<li class='right-down width2'><span>&nbsp;</span></li>
			</ul>
		</div>
		<div class="row2 size2 operation ${step.operation}" style="left:${stepNum * 18}em; top: 5em; border: none">
			<c:if test="${step.nextStep != null}">
				<ul>
					<li class='right width15'><span>&nbsp;</span></li>
				</ul>
			</c:if>
		</div>
	</c:otherwise>
</c:choose>
