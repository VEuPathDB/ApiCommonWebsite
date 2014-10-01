<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="api" uri="http://apidb.org/taglib" %>
<%@ taglib prefix="w" uri="http://www.servletsuite.com/servlets/wraptag" %>

<%-- Get proper project scope --%>
<c:set var="wdkRecord" value="${requestScope.wdkRecord}" />
<c:set var="attrs" value="${wdkRecord.attributes}" />
<c:set var="props" value="${applicationScope.wdkModel.properties}" />

<c:set var="primaryKey" value="${wdkRecord.primaryKey}" />
<c:set var="pkValues" value="primaryKey.values" />
<c:set var="id" value="${pkValues['source_id']}"

<c:set var="recordName" value="wdkRecord.recordClass.displayName" />

<c:catch var="err">
<%-- force RecordInstance.fillColumnAttributeValues() to run
      and set isValidRecord to false if appropriate. 
      wdkRecord.isValidRecord is tested in the project's RecordClass --%>
<c:set var="junk" value="${attrs['name']}"/>
</c:catch>

<imp:pageFrame title="${id}" divisionName="" />
