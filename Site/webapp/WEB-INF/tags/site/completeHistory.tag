<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<%@ attribute name="model"
             type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
             required="false"
             description="Wdk Model Object for this site"
%>

<%@ attribute name="user"
              type="org.gusdb.wdk.model.jspwrap.UserBean"
              required="false"
              description="Currently active user object"
%>

<c:set var="steps" value="${user.steps}"/>
<c:set var="modelName" value="${model.name}"/>
<%-- <c:set var="showOrthoLink" value="${fn:containsIgnoreCase(modelName, 'plasmodb') || fn:containsIgnoreCase(modelName, 'apidb') || fn:containsIgnoreCase(modelName, 'cryptodb')}" /> --%>
<c:set var="invalidSteps" value="${user.invalidSteps}" />


<!-- decide if there are any steps -->
<c:choose>
  <c:when test="${user == null || fn:length(steps) == 0}">
  <div align="center">You have no searches in your history.  Please run a search from the <a href="/">home</a> page, or by using the "New Search" menu above, or by selecting a search from the <a href="/queries_tools.jsp">searches</a> page.</div>
  </c:when>
  <c:otherwise>

  <!-- begin display steps -->
  <div id="complete_history">
    <table border="0" cellpadding="5" cellspacing="0">
       <tr class="headerrow">
          <th>ID</th>
          <th>Query</th>
          <th>Type</th>
          <th>Date</th>
          <th>Version</th>
          <th>Size</th>
          <th>&nbsp;</th>
       </tr>
       <c:forEach items="${steps}" var="step">
         <c:set var="type" value="${step.dataType}"/>
         <c:set var="isGeneRec" value="${fn:containsIgnoreCase(type, 'GeneRecordClass')}"/>
         <c:set var="recDispName" value="${step.answerValue.question.recordClass.type}"/>
         <c:set var="recTabName" value="${fn:substring(recDispName, 0, fn:indexOf(recDispName, ' ')-1)}"/>
         
         <c:choose>
           <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
           <c:otherwise><tr class="linesalt"></c:otherwise>
         </c:choose>
            <td>${step.stepId}</td>
            <c:set var="dispName" value="${step.answerValue.question.displayName}"/>
            <td width=450>${dispName}</td>
            <td width=450>${recDispName}</td>
	    <td align='right' nowrap>${step.createdTime}</td>
	    <td align='right' nowrap>
	    <c:choose>
	      <c:when test="${step.version == null || step.version eq ''}">${wdkModel.version}</c:when>
              <c:otherwise>${step.version}</c:otherwise>
            </c:choose>
            </td>
            <td align='right' nowrap>${step.estimateSize}</td>
            <c:set value="${step.answerValue.question.fullName}" var="qName" />
            <td nowrap><a href="downloadUserAnswer.do?user_answer_id=${step.stepId}">download</a></td>
         </tr>
         <c:set var="i" value="${i+1}"/>
       </c:forEach>
       <!-- end of forEach step -->
    </table>
</div>
<!-- end of showing steps -->

  </c:otherwise>
</c:choose> 
<!-- end of deciding step emptiness -->

