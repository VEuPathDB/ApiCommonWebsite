<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>

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
    <form name="cuForm" method="post" action="communityUpload.do" 
          enctype="multipart/form-data">
          
         Select File: <input type="file" name="file"><p/>
         <input type="submit" value="Upload File">

    </form>

    </c:otherwise>
</c:choose>