<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="modelName" value="${applicationScope.wdkModel.name}" />
<c:set var="CGI_URL" value="${wdkModel.properties['CGI_URL']}"/>
<c:set var="sName" value="${fn:substringBefore(modelName,'DB')}" />
<c:set var="cycName" value="${sName}Cyc" />
<div id="info">
    	<ul>
		<li><a href="<c:url value="/showQuestion.do?questionFullName=UniversalQuestions.UnifiedBlast"/>"><strong>BLAST</strong></a>
			<ul><li>Identify Sequence Similarities</li></ul>
		</li>
		<li><a href="<c:url value="/srt.jsp"/>"><strong>Sequence Retrieval</strong></a>
			<ul><li>Retrieve Specific Sequences using IDs and coordinates</li></ul>
		</li>
		<li><a href="/common/PubCrawler/"><strong>PubMed and Entrez</strong></a>
			<ul><li>View the Latest <i>Trypanosoma</i> and <i>Leishmania</i> Pubmed and Entrez Results</li></ul>
		</li>
		<li><a href="${CGI_URL}/gbrowse/${modelName}/"><strong>GBrowse</strong></a>
			<ul><li>View Sequences and Features in the GMOD Genome Browser</li></ul>
		</li>

<c:choose>
<c:when test="${sName != 'TriTryp'}">
		<li><a href="#"><strong>${cycName}</strong></a>
			<ul><li>Explore Automatically Defined Metabolic Pathways</li></ul>
		</li>
</c:when>
<c:otherwise>   <%----- fill in 3 empty lines to keep buckets aligned -----%>
	        <li>&nbsp;<ul><li>&nbsp;<br>&nbsp;</li></ul></li> 

</c:otherwise>
</c:choose>

    	</ul>
</div>
<div id="infobottom" class="tools">
</div><!--end info-->
