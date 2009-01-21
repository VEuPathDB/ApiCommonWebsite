<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>



<c:set var="project" value="${wdkModel.name}"/>

<%--Information message retrieved from DB via messaging system--%>
<c:set var="siteInfo">
  <site:announcement messageCategory="Information" projectName="${project}" />
</c:set>

<c:if test="${siteInfo != ''}">
<div class="infobox">
  <div class="warningIcon">
       <img src="/images/clearInfoIcon.png" alt="warningSign" />
  </div>
  <div class="warningMessage">
      ${siteInfo}
  </div>
</div>
</c:if>


<%--Retrieve from DB and display site degraded message scheduled via announcements system--%>
<c:set var="siteDegraded">
  <site:announcement messageCategory="Degraded" projectName="${project}" />
</c:set>

<c:if test="${siteDegraded != ''}">
<div class="warnbox">
  <div class="warningIcon">
       <img src="/images/warningSign.png" alt="warningSign" />
  </div>
  <div class="warningMessage">
      ${siteDegraded}
  </div>
</div>
</c:if>


<%--Retrieve from DB and display site down message scheduled via announcements system--%>
<c:set var="siteDown">
  <site:announcement messageCategory="Down" projectName="${project}" />
</c:set>

<c:if test="${siteDown != ''}">
<div class="errorbox">
  <div class="downIcon">
       <img src="/images/stopSign.png" alt="stopSign" />
  </div>
  <div class="downMessage">
       ${siteDown}
  </div></div>
</c:if>

