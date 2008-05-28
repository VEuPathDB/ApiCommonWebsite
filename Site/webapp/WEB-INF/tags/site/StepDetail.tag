<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>


<%@ attribute name="step"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step to be displayed by this tag"
%>

<c:set var="wdkAnswer" value="${requestScope.wdkAnswer}" />
  <div class="crumb_details">
	
   <p><b>Details:&nbsp;</b><pre>${step.details}</pre></p>

   <c:choose>
      <c:when test="${step.isFirstStep}">
          <p><b>Results:&nbsp;</b>${step.filterResultSize}</p>
      </c:when>
      <c:otherwise>
          <p><b>Step Results:&nbsp;</b>${step.filterResultSize}</p>
          <p><b>Query Results:&nbsp;</b>${step.subQueryResultSize}</p>
      </c:otherwise>
   </c:choose>
<!--
<c:if test="${wdkAnswer.resultSize == 0}">
             <c:if test="${fn:containsIgnoreCase(dispModelName, 'ApiDB')}">
                <site:apidbSummary/>
            </c:if>
</c:if>
-->
          
  </div><!--End Crumb_Detail-->