<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="w" uri="http://www.servletsuite.com/servlets/wraptag" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="wdkUser" value="${sessionScope.wdkUser}"/>

<!-- display page header with recordClass type in banner -->
<site:header refer="recordPage" banner="Favorite page"/>

<c:set var="allFavorites" value="${wdkUser.favorites}" /><%-- a map of (RecordClass, List<Favorite>) --%>
<c:choose>
    <c:when test="${fn:length(allFavorites) == 0}">
        <p>You don't have any favorite records. You can create favorite record on the record page.</p>
    </c:when>
    <c:otherwise> <%-- has favorites --%>
        <div id="favorites">
            <c:forEach var="fav_item" items="${allFavorites}">
                <c:set var="favorites" value="${fav_item.value}" /> <%-- a list of favorites of a record type --%>
                <c:set var="recordClass" value="${fav_item.key}" />
                <h3>My Favorite ${recordClass.type}s (${fn:length(favorites)} ${recordClass.type}s)</h3>
                <table class="favorite-list" width="93%" border="1">
                    <tr><%--<th>&nbsp;</th>--%><th>${recordClass.type}s</th><th>My note</th><th>My project</th></tr>
                    <c:forEach var="favorite" items="${favorites}">
                        <c:set var="record" value="${favorite.recordInstance}" />
                        <c:set var="inbask" value="${record.inBasket}"/>
						<c:set var="primaryKey" value="${record.primaryKey}"/>
                        <c:set var="pkValues" value="${primaryKey.values}" />
                        <c:set value="${pkValues['source_id']}" var="id"/>
                        <c:set value="${pkValues['project_id']}" var="pid"/>
                        <tr class="wdk-record" recordClass="${recordClass.fullName}">
                            <td width="10%">
                                <div class="primaryKey">
                                    <c:forEach var="pk_item" items="${pkValues}">
                                        <span key="${pk_item.key}">${pk_item.value}</span>
                                    </c:forEach>
                                </div>
                                <img class="clickable" src="<c:url value='/wdk/images/favorite_color.gif'/>" 
                                     title="Click to remove this item from the Favorite."
                                     onClick="updateFavorite(this, 'remove')"/>
									
                            <%--</td>
                            <td>--%>
                                <c:set var="url" value="/showRecord.do?name=${recordClass.fullName}" />
                                <c:forEach var="pk_item" items="${pkValues}">
                                    <c:set var="url" value="${url}&${pk_item.key}=${pk_item.value}" />
                                </c:forEach>
                                <a href="<c:url value='${url}' />">${primaryKey.value}</a>
                            </td>
                            <td width="57%">
								<c:set var="favNote" value="${favorite.note}"/>
                                <span class="favorite-note">${favNote}</span>
                                <div class="favorite-button-div"><a href="javascript:void(0)" class="favorite-note-button" onClick="showInputBox(this, 'note', 'updateFavoriteNote(this)')" >edit</a></div>
                            </td>
                            <td>
								<c:set var="favGroup" value="${favorite.group}"/>
								<c:set var="favGroupStyle" value=""/>
								<c:if test="${fn:length(favGroup) == 0}">
									<c:set var="favGroup" value="Click edit to add a project"/>
									<c:set var="favGroupStyle" value="opacity:0.2"/>
								</c:if>
                                <span class="favorite-group" style="${favGroupStyle}">${favGroup}</span>
                                <a href="javascript:void(0)" class="favorite-group-button" onClick="showInputBox(this, 'group', 'updateFavoriteGroup(this)')">edit</a>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
             </c:forEach>
        </div>
		<div id="groups-list" style="display:none">
			<ul>	
			</ul>
		</div>
    </c:otherwise> <%-- END has favorites --%>
</c:choose>

<site:footer/>
