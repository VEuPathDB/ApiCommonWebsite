<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="w" uri="http://www.servletsuite.com/servlets/wraptag" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<c:set var="modelName" value="${wdkModel.displayName}"/>

<table width="100%" border="0" cellspacing="1" cellpadding="1">
<tr>


        <td width="50%" >
<div class="innertube2">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">

 <tr>
                    <imp:queryGridMakeUrl qset="PersonQuestions" qname="SubjectsByMetadata" linktext="Characteristics" existsOn="A Am G C M Pi P T Tr Tt"/>
                </tr>

            </table>
</div>
        </td>


</tr>
</table>
