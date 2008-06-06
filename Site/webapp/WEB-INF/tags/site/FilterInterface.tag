
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
<c:set var="catMap" value="${model.questionsByCategory}" />
<c:set var="recClass" value="${recordClass.fullName}" />
<c:set var="qSetName" value="none" />
<c:set var="qSets" value="${model.questionSetsMap}" />
<c:set var="qSet" value="${qSets[qSetName]}" />
<c:set var="qByCat" value="${qSet.questionsByCategory}" />



<script type="text/javascript" src="js/lib/jqDnR.js"></script>
<a class="redbutton" onclick="this.blur()" href="#" id="filter_link" style="position:relative; top: -4.1em; left: ${protocol.length * 11.65 + 5}em; color: #ffffff;"><span>Add Step</span></a>
<div id="filter_div">
<span id="instructions">Choose a query to use as a filter from the list below.  The individual queries will expand when you mouse over the categories.</span>

<div id="query_selection">
<ul class="top_nav">

<c:set var="qByCat" value="${catMap[recordClass.fullName]}" />
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

