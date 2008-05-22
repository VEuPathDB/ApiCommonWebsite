

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

<link rel="stylesheet" type="text/css" href="../assets/css/filter_menu.css">

<c:choose>
	<c:when test="${step.isFirstStep}">
		<div class="arrowgrey"><a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNumber}">${step.customName}</a>
	</c:when>
	<c:otherwise>
		<div class="operation ${step.operation}"></div>
		</td>
		<td>
			<div class="arrowgrey"><a class="crumb_name" href="showSummary.do?protocol=${protocol.protocolId}&step=${stepNumber}">${step.customName}</a>
	</c:otherwise>
</c:choose>
