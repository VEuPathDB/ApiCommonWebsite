<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<c:set var="ftype" value="Download"/>
<c:if test="${!empty param.ftype}">
  <c:set var="ftype" value="${param.ftype}"/>
</c:if>

<%-- used by gbrowse and other pages via /html/include/fancy*IndexHeader.shtml ----%>
<site:header     banner="${project} ${ftype} Files"
                 isBannerImage="${isbannerimage}"
                 bannerSuperScript="<br><b><font size=\"+1\">Release ${version}</font></b>"
                 division="downloads"/>

