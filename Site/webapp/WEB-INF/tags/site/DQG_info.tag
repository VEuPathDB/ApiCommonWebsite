<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="modelName" value="${applicationScope.wdkModel.name}" />
<c:set var="sName" value="${fn:substringBefore(modelName,'DB')}" />
<c:set var="cycName" value="${sName}Cyc" />

<%-- This tag file is NOT in use in current sites --%>
<%-- The original purpose of this tag is to have stats info or other in the portal --%>
<div id="info">
    	<ul>
		<li><a href="<c:url value="/showQuestion.do?questionFullName=UniversalQuestions.UnifiedBlast"/>"><strong>BLAST</strong></a>
			<ul><li>Description</li></ul>
		</li>
		<li><a href="<c:url value="/srt.jsp"/>"><strong>Sequence Retrieval</strong></a>
			<ul><li>Description</li></ul>
		</li>
		<li><a href="/common/PubCrawler/"><strong>PubMed and Entrez</strong></a>
			<ul><li>Description</li></ul>
		</li>
		<li><a href="#"><strong>GBrowse</strong></a>
			<ul><li>Description</li></ul>
		</li>
		<li><a href="#"><strong>${cycName}</strong></a>
			<ul><li>Description</li></ul>
		</li>
    	</ul>
</div>
<div id="infobottom">
</div><!--end info-->
