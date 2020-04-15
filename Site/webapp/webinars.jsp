<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<c:set var="wdkModel" value="${applicationScope.wdkModel}"/>
<imp:pageFrame title="${wdkModel.displayName} : Webinars"
                 banner="Webinars"
                 parentDivision="${wdkModel.displayName}"
                 parentUrl="/home.jsp"
                 divisionName="Webinars"
                 division="Webinars">

<div id="includedContent" class="eupathdb-content"></div>

<script> 
    $(function(){
     $("#includedContent").load("${wdkModel.model.properties.COMMUNITY_SITE}/webinars.html"); 
    });
</script> 

</imp:pageFrame>

