<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="pg" uri="http://jsptags.com/tags/navigation/pager" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>

<!-- get wdkAnswer from requestScope -->
<c:set value="${requestScope.wdkAnswer}" var="wdkAnswer"/>

<!-- display page header with wdkAnswer's recordClass's type as banner -->
<c:set value="${wdkAnswer.recordClass.type}" var="wdkAnswerType"/>
<!-- site:header banner="${wdkAnswerType} Results" / -->

<!-- handle empty result set situation -->
<c:choose>
  <c:when test='${wdkAnswer.resultSize == 0}'>
    No results for your query
  </c:when>
  <c:otherwise>

<!-- content of current page -->
<pre>${requestScope.downloadResult}</pre>

  </c:otherwise>
</c:choose>

<!-- site:footer/ -->
