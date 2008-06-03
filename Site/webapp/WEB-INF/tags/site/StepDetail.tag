<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>


<%@ attribute name="step"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step to be displayed by this tag"
%>

<c:set value="${step.filterHistory.answer.question.fullName}" var="questionName" />
<c:set value="${step.filterHistory.answer.questionUrlParams}" var="urlParams"/>

  <!--<div class="crumb_details">-->
	<p>Details:<pre>${step.details}</pre></p>

   <c:choose>
      <c:when test="${step.isFirstStep}">
          <p><b>Results:&nbsp;</b>${step.filterResultSize}</p>
      </c:when>
      <c:otherwise>
          <p><b>Query Results:&nbsp;</b>${step.subQueryResultSize}</p>
      </c:otherwise>
   </c:choose>
   <div class="crumb_menu">
		<a href="#">view</a>&nbsp;|&nbsp;<a href="showQuestion.do?questionFullName=${questionName}${urlParams}&questionSubmit=Get+Answer&goto_summary=0">edit</a>&nbsp;|&nbsp;<a href="#">delete</a>
   </div>       
 <!-- </div>End Crumb_Detail-->
