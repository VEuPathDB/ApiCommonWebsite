<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="wdkModel" value="${applicationScope.wdkModel}" />
<c:set var="rootCatMap" value="${wdkModel.rootCategoryMap}" />

	<ul>
		<c:forEach items="${rootCatMap}" var="rootCatEntry">
		    <c:set var="recType" value="${rootCatEntry.key}" />
		    <c:set var="rootCat" value="${rootCatEntry.value}" />
		  <c:if test="${recType == 'GeneRecordClasses.GeneRecordClass' || recType == 'SequenceRecordClasses.SequenceRecordClass'  || recType == 'OrfRecordClasses.OrfRecordClass' || recType == 'EstRecordClasses.EstRecordClass' || recType == 'IsolateRecordClasses.IsolateRecordClass' || recType == 'SnpRecordClasses.SnpRecordClass' || recType == 'AssemblyRecordClasses.AssemblyRecordClass' || recType == 'SageTagRecordClasses.SageTagRecordClass' }">
		 <c:choose>
		  <c:when test="${recType=='GeneRecordClasses.GeneRecordClass'}">
			<li><a href="#">Search for Genes</a>
				<ul>
					<c:forEach items="${rootCat.children}" var="catEntry">
					    <c:set var="cat" value="${catEntry.value}" />
                                            <c:if test="${fn:length(cat.questions) > 0}">
						<li>
							<a href="javascript:void(0)">${cat.displayName}</a>
							<ul>
								<c:forEach items="${cat.questions}" var="q">
									<li><a href="<c:url value="/showQuestion.do?questionFullName=${q.fullName}&target=GENE"/>">${q.displayName}</a></li>
								</c:forEach>
							</ul>
						</li>
                                            </c:if>
					</c:forEach>
				</ul>
			</li>
		  </c:when>
		  <c:otherwise>
			<c:set var="qByCat" value="${catByRec.value}" />
			<c:forEach items="${rootCat.children}" var="catEntry">
			    <c:set var="cat" value="${catEntry.value}" />
                            <c:if test="${fn:length(cat.questions) > 0}">
			<li><a href="#">${cat.displayName}</a> 
				<ul>
				<c:forEach items="${cat.questions}" var="q">
				    <li><a href="<c:url value="/showQuestion.do?questionFullName=${q.fullName}"/>">${q.displayName}</a></li>
				</c:forEach>
				</ul>
			</li>
                            </c:if>
			</c:forEach>
		  </c:otherwise>
		 </c:choose>
		 </c:if>
		</c:forEach>

        <li><a href="<c:url value="/queries_tools.jsp"/>">View all available searches</a></li>
	</ul>
