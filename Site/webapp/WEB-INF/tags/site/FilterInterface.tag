
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bean" uri="http://jakarta.apache.org/struts/tags-bean" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<%@ attribute name="model"
	      type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
              required="false"
              description="Wdk Model Object for this site"
%>

<%@ attribute name="recordClass"
	      type="org.gusdb.wdk.model.jspwrap.RecordClassBean"
              required="false"
              description="RecordClass Object for the Answer"
%>

<%@ attribute name="protocol"
	      type="org.gusdb.wdk.model.jspwrap.ProtocolBean"
              required="false"
              description="protocol for this result"
%>
<span id="proto" style="display: none">${protocol.protocolId}</span>
<c:set var="recClass" value="${recordClass.fullName}" />
<c:set var="qSetName" value="none" />
<c:choose>
	<c:when test="${fn:containsIgnoreCase(recClass,'GeneRecordClass')}">
		<c:set var="qSetName" value="GeneQuestions" />
	</c:when>
	<c:when test="${fn:containsIgnoreCase(recClass,'SequenceRecordClass')}">
		<c:set var="qSetName" value="GenomicSequenceQuestions" />
	</c:when>
	<c:when test="${fn:containsIgnoreCase(recClass,'EstRecordClass')}">
		<c:set var="qSetName" value="EstQuestions" />
	</c:when>
	<c:when test="${fn:containsIgnoreCase(recClass,'SnpRecordClass')}">
		<c:set var="qSetName" value="SnpQuestions" />
	</c:when>
	<c:when test="${fn:containsIgnoreCase(recClass,'IsolateRecordClass')}">
		<c:set var="qSetName" value="IsolateQuestions" />
	</c:when>
	<c:when test="${fn:containsIgnoreCase(recClass,'AssemblyRecordClass')}">
		<c:set var="qSetName" value="AssemblyQuestions" />
	</c:when>
	<c:otherwise>
		<c:set var="qSetName" value="NADA..NOTHING...NILL" />
	</c:otherwise>
</c:choose>
<c:set var="qSets" value="${model.questionSetsMap}" />
<c:set var="qSet" value="${qSets[qSetName]}" />
<c:set var="qByCat" value="${qSet.questionsByCategory}" />



<script type="text/javascript" src="js/lib/jqDnR.js"></script>
<a class="arrowred row2" href="#" id="filter_link" style="position:relative; top: -5em; left: ${protocol.length * 11.65 + 5}em">Create Filter</a>
<div id="filter_div">
<span id="instructions">Choose a query to use as a filter from the list below.  The individual queries will expand when you mouse over the categories.</span>

<div id="query_selection">
<ul class="top_nav">


<c:forEach items="${qByCat}" var="cat">
	<li><a class="category" href="javascript:void(0)">${cat.key}</a>
	<ul>
	<c:forEach items="${cat.value}" var="q">
		<li><a href="showQuestion.do?questionFullName=${q.fullName}">${q.displayName}</a></li>
	</c:forEach>
	</ul>
</c:forEach>

</ul>
</div><!-- End of Query Selection Div -->
<div id="query_form" class="jqDnR">
</div><!-- End of Query Form Div -->
</div><!-- End of Filter div -->

