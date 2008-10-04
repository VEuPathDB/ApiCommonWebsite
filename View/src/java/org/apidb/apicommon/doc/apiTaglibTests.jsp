<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- load ApiCommonWebsite taglibs --%>
<%@ taglib prefix="api" uri="http://apidb.org/taglib" %>

<head>
	<style type="text/css">
    blockquote {
		margin: 20px;
		padding: 0px;
		background: #CACCB4;
		font: 12px courier, mono-spaced; }
    .fail {
		margin: 20px;
		padding: 0px;
		background: red;
		font: 12px courier, mono-spaced; }
    </style>
</head>
<html>
<body>

<%-- --%>
<h2 align='center'>ApiCommonWebsite taglib Examples</h2>
<i><b>Check for </b></i> <code>-- PAGE COMPLETE --</code> <i><b>at the end of this page to confirm that page
has fully rendered.</b></i>

<h3>api:wdkRecord</h3>

wdkRecord tag instantiates a wdk record for a given record name and can be used within any 
JSP page, avoiding the need to have the JSP file be named after the record class. More than 
one record can be instantiated in a single JSP page.
<p>
Two records, GeneRecordClasses.GeneRecordClass (generec) and 
UtilityRecordClasses.SiteInfo (wdkRecord) co-mingled in same page.
<p>
<%-- 
Record 1, with defaults recordKey=wdkRecord, source_id=' ', project_id=' '
--%>
<api:wdkRecord name="UtilityRecordClasses.SiteInfo"/>
<%-- 
Record 2, with explicit param settings. use of <rtexprvalue> in source_id
--%>
<c:set var='source_id' value='TGME49_039250'/>
<api:wdkRecord name="GeneRecordClasses.GeneRecordClass" 
    recordKey="generec" source_id="${source_id}" project_id='ToxoDB' />
<c:set var="geneattrs" value="${generec.attributes}"/>
<c:set var="attrs"     value="${wdkRecord.attributes}"/>

<blockquote>
wdkRecord source_id (quoted): '<b>${wdkRecord.primaryKey.values.source_id}</b>'
<br>
generec source_id (quoted): '<b>${generec.primaryKey.values.source_id}</b>'
<br>
wdkRecord projectId: <b>${wdkRecord.primaryKey.values['project_id']}</b>
<br>
generec projectId: <b>${wdkRecord.primaryKey.values['project_id']}</b>
<br>
wdkRecord db global_name: <b>${fn:toLowerCase(wdkRecord.attributes['global_name'].value)}</b>
</blockquote>

<p>applicationScope.wdkModel, provided by the webapp, is accessible in this page scope, 
allowing access to properties from model.prop<br>
<%-- applicationScope.wdkModel is accessible in this page scope --%>
<c:set var="props" value="${applicationScope.wdkModel.properties}" />
<blockquote>
LOGIN_SCHEMA from model.prop: <b>${props['LOGIN_SCHEMA']}</b>
</blockquote>

<hr>
<h3>api:table</h3>
<p>
<blockquote>
<table border="1" cellspacing="3" cellpadding="2" align="${align}">

<%-- table header --%>
<api:table var="tbl" tableName="AllDbLinks">

  <tr class="secondary3">
  <api:whileColumnHeader var="hCol">
  
    <c:if test="${!hCol.internal}">
    <th align="left"><font size="-2">${hCol.displayName}</font></th>
    </c:if>
  
  </api:whileColumnHeader>
  </tr>

  <c:set var="i" value="0"/>

  <api:whileRow var="row">
    <c:choose>
    <c:when test="${i % 2 == 0}"><tr class="rowLight"></c:when>
    <c:otherwise><tr class="rowMedium"></c:otherwise>
    </c:choose>

    <api:whileColumn var="col">
        <c:choose>
            <c:when test="${col.class.name eq 'org.gusdb.wdk.model.AttributeFieldValue' && col.name eq 'pval'}">
                <td nowrap="nowrap">
            </c:when>
            <c:when test="${col eq 'pval'}">
                <td nowrap="nowrap">
            </c:when>
            <c:otherwise>
                <td>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${col.class.name eq 'org.gusdb.wdk.model.LinkValue'}">
                <a href="${col.url}">${col.visible}</a>
            </c:when>
            <c:when test="${col.class.name eq 'org.gusdb.wdk.model.AttributeFieldValue'}">
                ${col.value}
            </c:when>
            <c:otherwise>
                ${col}
            </c:otherwise>
        </c:choose>
    </api:whileColumn>

   </tr>
   <c:set var="i" value="${i +  1}"/>
 </api:whileRow>

</api:table>
</table>
</blockquote>

<hr>

<h3>api:modelConfig</h3>
<api:modelConfig var="modelConfig"/>
<blockquote>
authenticationConnectionUrl: <b>${modelConfig.props['authenticationConnectionUrl']}</b>
</blockquote>
<p>
Configurations from WDK's ModelConfig class, generally representing values set in <code>${modelConfig.props['gusHome']}/config/${wdkRecord.primaryKey.values['project_id']}/model-config.xml</code> although some processing may occur by the WDK parser.
<pre>
<blockquote>
<c:forEach 
    var="section" items="${modelConfig.props}"
><b>${section.key}</b><blockquote><c:forEach 
    var="cfg" items="${section.value}"
>${cfg.key} = ${fn:escapeXml(cfg.value)}
</c:forEach></blockquote></c:forEach>
</blockquote>
</pre>
<p>
<hr>

<h3>api:commentConfig</h3>
not yet available, waiting for fix in WdkModel
<%--
<api:commentConfig var="commentConfig"/>
<blockquote>
connectionUrl: <b>${commentConfig.props['connectionUrl']}</b>
</blockquote>
<p>
Configurations from WDK's CommentConfig class, generally representing values set in <code>${modelConfig.props['gusHome']}/config/${wdkRecord.primaryKey.values['project_id']}/comment-config.xml</code> although some processing may occur by the WDK parser.
The CommentFactory instantiates CommentConfig on first page access requiring it - not at webapp load time.
<pre>
<blockquote>
<c:forEach 
    var="cfg" items="${commentConfig.props}"
>${cfg.key} = ${fn:escapeXml(cfg.value)}
</c:forEach>
</blockquote>
</pre>
<p>
<hr>
--%>

<h3>api:wdkCacheDB</h3>

<api:wdkCacheDB var="cacheTable"/>
<blockquote>
Cache database service_name: <b>${cacheTable.dbInfo['service_name']}</b>
</blockquote>
<br>

<hr>
<h3>api:properties</h3>
<api:properties var="build" propfile="WEB-INF/wdk-model/config/.build.info" />
Parse Java property file into page scope. Example reading WEB-INF/wdk-model/config/.build.info
<blockquote>
Last.build.timestamp: <b>${build['!Last.build.timestamp']}</b>
</blockquote>
<hr>
<h3>api:configurations</h3>
parse projectId and url from apifed-config.xml

<c:catch var="e">
<api:configurations var="config" configfile="WEB-INF/wdk-model/config/apifed-config.xml.sample" />
<blockquote>
<pre>
<c:forEach 
    items="${config}" var="cfg"
>${cfg.key} = ${fn:escapeXml(cfg.value)}
</c:forEach>
</pre>
</blockquote>
</c:catch>
<c:if test="${ e != null }">
   <blockquote class='fail'>
    fail: ${e}
    </blockquote>
</c:if>


<hr>
<h3>api:httpstatus</h3>
Design use-case is to check if a site is up.
      Only checks the http header (or timeout trying), no page content is returned
      from the server. So, we check only that the server is responding, not
      whether it is responding correctly.
      Our sites wrap WDK exceptions in valid JSP, so "200 OK" Status returns
      are normal. Catching read/connect timeouts and 5XX status codes is
      the best we can do - and generally sufficient.
   <c:set var="url">
    http://plasmodb.org/
   </c:set>
   <c:catch var="e">
     <api:httpstatus
        url="${url}"
        var="statusCode"
        readTimeout="5000"
        connectTimeout="2000"
        followRedirect="true"
     />
       <blockquote>
            Checking ${url}<br>
            status code: ${statusCode}
       </blockquote>
   </c:catch>
   <c:if test="${ e != null || fn:startsWith(statusCode, '5') }">
        <blockquote class='fail'>
        fail, status code: ${statusCode}
        </blockquote>
   </c:if>

<hr>

<h3>api:webappInfo</h3>
<api:webappInfo var="app"/>
<blockquote>
Instance Name: ${app.instanceName}<br>
Instance Uptime: ${app.instanceUptimeText}<br>
Webapp Uptime: ${app.webappUptimeText}<br>
Webapp context path: ${app.contextPath}<br>
Webapp classpath (truncated): ${fn:substring(app.classpath, 0, 100)} ...<br>

</blockquote>
<hr>
<code>-- PAGE COMPLETE --</code><%-- confirm page is fully rendered --%>
</body>

</html>