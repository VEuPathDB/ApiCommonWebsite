<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="dispModelName" value="${applicationScope.wdkModel.displayName}" />
 <c:if test="${fn:containsIgnoreCase(dispModelName, 'ApiDB')}">
     <c:set var="dispModelName" value="EupathDB" />
</c:if>

<div align="center">

<div id="footer">
    <a href="http://www.eupathdb.org"><img src="/assets/images/eupathdblink.png" alt="Link to EuPathDB homepage" width="144" height="25" align="right" border='0' /></a>	&copy;2008  The EuPath Project Team:: 
    <a href="http://${fn:toLowerCase(dispModelName)}.org">${dispModelName}.org</a> <br />
    Please <a href="<c:url value="/help.jsp"/>">Contact Us</a> with any questions or concerns.
</div>

</div>
</body>
</html>
