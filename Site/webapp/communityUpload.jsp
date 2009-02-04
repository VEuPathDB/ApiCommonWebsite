<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="bean" uri="http://jakarta.apache.org/struts/tags-bean" %>

<c:set var="wdkUser" value="${sessionScope.wdkUser}"/>
<c:set var="wdkModel" value="${applicationScope.wdkModel}"/>

<site:header title="${wdkModel.displayName}.org :: Community Upload"
             banner="Community Upload"/>
<head>
</head>

<c:choose>
	<c:when test="${empty wdkUser || wdkUser.guest}">
		<p align=center>Please login to upload files.</p>
		<table align='center'><tr><td><site:login/></td></tr></table>
	</c:when>
	
	<c:otherwise>
    <html:form method="post" action="/communityUpload.do" 
          enctype="multipart/form-data">
          
    <wdk:errors/>
         Select File: <html:file property="file" /><p/>
         <p>
     Description: <html:textarea rows="5" cols="50" property="notes"/>

        <p>
        <html:submit property="submit" value="Upload File"/>
    </html:form>

    </c:otherwise>
</c:choose>