<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<!-- get wdkQuestion; setup requestScope HashMap to collect help info for footer -->  
<c:set value="${requestScope.wdkQuestion}" var="wdkQuestion"/>
<jsp:useBean scope="request" id="helps" class="java.util.LinkedHashMap"/>

<!-- display page header with wdkQuestion displayName as banner -->
<site:header title="Queries & Tools :: BLAST Question"
                 banner="Identify ${wdkQuestion.recordClass.type}s based on ${wdkQuestion.displayName}"
                 parentDivision="Queries & Tools"
                 parentUrl="/showQuestionSetsFlat.do"
                 divisionName="BLAST Question"
                 division="queries_tools"/>
<script type="text/javascript">

function CalculateBlastAlgorithm(){
    	var queryType = document.getElementById('BlastQueryType');
   	var targetType = document.getElementById('BlastDatabaseType');
	var qType = queryType.options[queryType.selectedIndex].text;
	var dbType = targetType.options[targetType.selectedIndex].text;
	var bp;
//	if (qType.toLowerCase() == "dna") {
//            if (dbType.toLowerCase() == "proteins" || dbType.toLowerCase() == "orf" ) {
//		bp = "blastx";
//	    } else if (dbType.toLowerCase() == "translated") {
 //               bp = "tblastx";
//	    } else { bp = "blastn"; }
//	    
//	} else if (qType.toLowerCase() == "protein") {
 //           if ( dbType.toLowerCase() == "proteins" || dbType.toLowerCase().toLowerCase() == "orf" ) {
//		bp = "blastp";
 //           } else { bp = "tblastn"; }
//	}
	
	if (qType.search(/dna/i) != -1) {
            if (dbType.search(/proteins/i) != -1 || dbType.search(/orf/i) != -1) {
		bp = "blastx";
	    } else if (dbType.search(/translated/i) != -1) {
                bp = "tblastx";
	    } else { bp = "blastn"; }
	    
	} else if (qType.search(/protein/i) != -1) {
            if ( dbType.search(/proteins/i) != -1 || dbType.search(/orf/i) != -1 ) {
		bp = "blastp";
            } else { bp = "tblastn"; }
	}
	
	document.getElementById('BlastAlgorithm').value = bp;

}

</script>
<table border=0 width=100% cellpadding=3 cellspacing=0 bgcolor=white class=thinTopBottomBorders> 

 <tr>
  <td bgcolor=white valign=top>

<!-- show all params of question, collect help info along the way -->
<c:set value="Help for question: ${wdkQuestion.displayName}" var="fromAnchorQ"/>
<jsp:useBean id="helpQ" class="java.util.LinkedHashMap"/>

<!-- put an anchor here for linking back from help sections -->
<A name="${fromAnchorQ}"></A>
<html:form method="post" action="/processQuestion.do"> 
<input type="hidden" name="questionFullName" value="${wdkQuestion.fullName}"/>
<table>

<!-- show error messages, if any -->
<wdk:errors/>


<c:set value="${wdkQuestion.params}" var="qParams"/>
<c:forEach items="${qParams}" var="qP">

  <!-- an individual param (can not use fullName, w/ '.', for mapped props) -->
  <c:set value="${qP.name}" var="pNam"/>
  <tr><td align="right"><b>
<c:if test="${qP.isVisible == true}">
<jsp:getProperty name="qP" property="prompt"/></b></td>
</c:if>
  <!-- choose between flatVocabParam and straight text or number param -->
  <c:choose>
    <c:when test="${qP.class.name eq 'org.gusdb.wdk.model.jspwrap.FlatVocabParamBean'}">
      <td>
        <site:flatVocabParamInput qp="${qP}" />
      </td>
    </c:when>
    <c:otherwise>
      <td>
        <c:choose>
          <c:when test="${pNam == 'BlastQuerySequence'}">
            <html:textarea property="myProp(${pNam})"  styleId="${qP.id}" cols="50" rows="4"/>
          </c:when>
   <%--       <c:when test="${pNam == 'BlastAlgorithm'}">
            <input name="myProp(${pNam})"  id="BlastAlgorithm" type="hidden"/>
          </c:when>--%>
          <c:otherwise> 
          <c:choose>
          <c:when test="${qP.isVisible == false}">
            <html:hidden property="myProp(${pNam})" styleId="${qP.id}" />
          </c:when>
	  <c:otherwise>
	    <html:text property="myProp(${pNam})" styleId="${qP.id}" />
          </c:otherwise>
          </c:choose>
          </c:otherwise>
        </c:choose>
      </td>
    </c:otherwise>
  </c:choose>

      <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
      <td>
          <c:set var="anchorQp" value="HELP_${fromAnchorQ}_${pNam}"/>
          <c:set target="${helpQ}" property="${anchorQp}" value="${qP}"/>
          <a href="#${anchorQp}">
          <img src='<c:url value="/images/toHelp.jpg"/>' border="0" alt="Help!"></a>
      </td>
  </tr>
</c:forEach>
<c:set target="${helps}" property="${fromAnchorQ}" value="${helpQ}"/>

  <tr><td><html:hidden property="altPageSize" value="1000000"/></td>
  		<td>
  		<table><tr>
  		<td><html:submit property="questionSubmit" onclick="CalculateBlastAlgorithm()" value="Get Answer"/></td>
		<td><input type="button" value="Clear Sequence" onClick="this.form.elements[4].value='';"/></td>
		<td><html:reset>Reset All</html:reset></td>
        </tr></table>
        </td></tr>
</table>
</html:form>

<hr>

<!-- display description for wdkQuestion -->
<p><b>Query description:</b> <jsp:getProperty name="wdkQuestion" property="description"/></p>

  </td>
  <td valign=top class=dottedLeftBorder></td> 
</tr>
</table> 

<site:footer/>
