<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ attribute name="name"
              required="true"
              description="Internal, URL-safe name of toggle block"
%>

<%@ attribute name="displayName"
              required="true"
              description="Value to appear at top of page"
%>
<%@ attribute name="dsLink"
              required="false"
              description="data sources link for the genomic context, links to GBrowse file that includes help and track descriptions"
%>

<%@ attribute name="displayLink"
              required="false"
              description="Work around on toggle-handle for hyperlink"
%>

<%@ attribute name="content"
              required="true"
              description="text appearing inside toggle block in 'show' mode"
%>

<%@ attribute name="anchorName"
              required="false"
              description="An anchor for the toggle to land on after toggling. Without this we will lose page scrolling in Firefox/Netscape browsers"
%>

<%@ attribute name="imageId"
              required="false"
              description="the id of a image in the content"
%>

<%@ attribute name="imageSource"
              required="false"
              description="the src of a image in the content"
%>

<%@ attribute name="imageMapDivId"
              required="false"
              description="the id of a image-map-enclosing div in the content"
%>

<%@ attribute name="imageMapSource"
              required="false"
              description="the src of a image map in the content"
%>

<%@ attribute name="postLoadJS"
              required="false"
              description="comma separated list of javascript to load, in the order listed, after imageMapSource loads"
%>

<%@ attribute name="isOpen"
              required="false"
              description="Whether toggle block should initially be open"
%>

<%@ attribute name="noData"
              required="false"
              description="Is there nothing to display?"
%>

<%@ attribute name="attribution"
              required="false"
              description="Dataset ID (from Data Sets) for attribution"
%>
<%@ attribute name="downloadLink"
              required="false"
              description="download link"
%>

<c:set var="userAgent" value="${header['User-Agent']}"/>

<%-- most CSS selector characters (., >, +, ~, #, :, etc) are not valid in id attributes or tag names --%>
<%-- but some of the names used in gene page contain . or : .......  remove or escape them \\ --%>
<%--
<c:set var="name" value="${fn:replace(name, '.', '')}"/>
<c:set var="name" value="${fn:replace(name, ':', '')}"/>
--%>

<!-- allow user's previous setting (in cookie: section open or closed) to override default in database -->
<c:set var="cookieKey" value="show${name}"/>
<c:set var="userPref" value="${cookie[cookieKey].value}"/>

<%-- 	- check cookie state
	- if cookie not found, check if isOpen was specified in table (passed to tag as an attribute)
	- otherwise isOpen will be set to false (closed section)
--%>
<c:choose>
<%-- found cookie --%>
<c:when test='${not empty userPref}'>
	<c:if test="${userPref == '1'}"><c:set var="isOpen" value="true"/></c:if>
	<c:if test="${userPref == '0'}"><c:set var="isOpen" value="false"/></c:if>
</c:when>
<%-- did not find cookie --%>
<c:otherwise>
	<c:if test='${empty isOpen}'>  
		<c:set var="isOpen" value="false"/>
	</c:if>
</c:otherwise>
</c:choose>

<c:set var="displayNameParam">
	<c:url value="${displayName}"/>
</c:set>

<c:set var="ds_ref_attribute" value="${requestScope.ds_ref_attributes[name]}" />
<c:set var="ds_ref_table" value="${requestScope.ds_ref_tables[name]}" />
<c:set var="ds_ref_profile_graph" value="${requestScope.ds_ref_profile_graphs[name]}" />
<c:set var="hasDBDataset" value="${(ds_ref_table != null && ds_ref_table != '') || (ds_ref_attribute != null && ds_ref_attribute != '') || (ds_ref_profile_graph != null && ds_ref_profile_graph != '')}" />

<c:set var="showOnClick" value=""/>
<c:if test="${imageId != null}">
  <c:set var="showOnClick" value="wdk.api.updateImage('${imageId}', '${imageSource}')"/>
</c:if>
<c:if test="${imageMapDivId != null}">
  <c:set var="showOnClick" value="wdk.api.updateImageMapDiv('${imageMapDivId}', '${imageMapSource}', '${postLoadJS}')"/>
</c:if>

<c:if test="${not dsLink}">
  <c:choose>
    <c:when test="${name != null && name !='' && hasDBDataset}">
      <c:set var="wdkRecord" value="${requestScope.wdkRecord}" />
      <c:set var="rcName" value="${wdkRecord.recordClass.fullName}" />
      <c:set var="dsLink" value="getDataset.do?reference=${name}&amp;recordClass=${rcName}&amp;display=detail"/>
    </c:when>
    <c:when test='${attribution != null && attribution != ""}'>
      <c:set var="dsLink" value="getDataset.do?display=detail&amp;datasets=${attribution}&amp;title=${displayNameParam}"/>
    </c:when>
  </c:choose>
</c:if>

<c:if test="${ anchorName == null}">
  <c:set var="anchorName" value="${name}"/>
  <a name="${anchorName}"></a>
</c:if>

<div class="toggle-section" wdk-active="${isOpen}" wdk-onactivate="${showOnClick}" wdk-id="${name}">
  <h3> ${displayName} </h3>
  <div>
    <c:if test="${not empty dsLink}">
      <div style="margin-bottom: 1em;">[ <a href="${dsLink}">Data Sets</a> ]</div>
    </c:if>
    ${content}
  </div>
</div>
