<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="siteName" value="${applicationScope.wdkModel.name}" />

<c:set var="wdkUser" value="${sessionScope.wdkUser}"/>

<c:choose>
<c:when test="${wdkUser.historyCount == null}">
<c:set var="count" value="0"/>
</c:when>
<c:otherwise>
<c:set var="count" value="${wdkUser.historyCount}"/>
</c:otherwise>
</c:choose>



<div id="menubar">

<ul id="nav">
<li><a href="<c:url value="/"/>">Home<img src="/assets/images/menu_divider3.png" width="20" height="11" /></a>
</li>
<li><a href="<c:url value="/queries_tools.jsp"/>">All Queries<img src="/assets/images/menu_divider3.png" width="20" height="11" /></a>
  <site:drop_down_QG />
</li>
<li>
	<a href="<c:url value="/showQueryHistory.do"/>">
		<div id="mysearch">My Searches: ${count}</div>
	</a>
</li>
<li><a href="#"><img src="/assets/images/menu_divider4.png" width="20" height="11" />Data Sources<img src="/assets/images/menu_divider.png" width="24" height="11" /></a>
  <ul>
    <li><a href="#">Data contained in CryptoDB</a></li>
    <li><a href="#">Analysis Methods</a></li>
    <li><a href="#">Standard Operating Procedures (SOPs)</a></li>
  </ul>
</li>
<li><a href="#">Download Files<img src="/assets/images/menu_divider.png" width="24" height="11" /></a></li>

<li><a href="#">About ${siteName}<img src="/assets/images/menu_divider.png" width="24" height="11" /></a>
  <ul>
    <li><a href="#">What is ${siteName}?</a></li>

    <li><a href="#">Data Releases and Updates</a></li>
    <li><a href="#">Other Recent News</a></li> 
    <li><a href="#">FAQs</a></li>
    <li><a href="#">Data Access Policy</a></li>
    <li><a href="#">Citing ${siteName}</a></li>
    <li><a href="#">Getting Support for ${siteName}</a></li>
    <li><a href="#">Community Links</a></li>
    <li><a href="#">Publications</a></li>
  </ul>
</li>

<li><a href="#">Log In/Register<img src="/assets/images/menu_divider.png" width="24" height="11" /></a></li>
<li><a href="#">Contact Us<img src="/assets/images/menu_divider2.png" width="2" height="11" /></a></li>
</ul>
</div>
