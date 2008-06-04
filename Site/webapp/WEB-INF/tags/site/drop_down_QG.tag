<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="CGI_URL" value="${wdkModel.properties['CGI_URL']}"/>
<c:set var="wdkModel" value="${applicationScope.wdkModel}" />
<c:set var="qSets" value="${wdkModel.questionSets}" />

	<ul>
		<c:forEach items="${qSets}" var="qSet">
		  <c:if test="${qSet.name == 'GeneQuestions' || qSet.name == 'GenomicSequenceQuestions' || qSet.name == 'EstQuestions' || qSet.name == 'SnpQuestions' || qSet.name == 'OrfQuestions' || qSet.name == 'IsolateQuestions'}">
			<li><a href="#">${qSet.displayName}</a>
				<ul>
					<c:choose>
						<c:when test="${qSet.name == 'GeneQuestions'}">
							<c:set var="qByCat" value="${qSet.questionsByCategory}" />
							<c:forEach items="${qByCat}" var="cat">
								<li>
									<a href="javascript:void(0)">${cat.key}</a>
									<ul>
										<c:forEach items="${cat.value}" var="q">
											<li><a href="showQuestion.do?questionFullName=${q.fullName}">${q.displayName}</a></li>
										</c:forEach>
									</ul>
								</li>
							</c:forEach>
						</c:when>
						<c:otherwise>
							<c:forEach items="${qSet.questions}" var="q">
								<li><a href="showQuestion.do?questionFullName=${q.fullName}">${q.displayName}</a></li>
							</c:forEach>
						</c:otherwise>
					</c:choose>
				</ul>
			</li>
		  </c:if>
		</c:forEach>
		<li><a href="#">Tools</a>
      		<ul>
        		<li><a href="<c:url value="/showQuestion.do?questionFullName=UniversalQuestions.UnifiedBlast"/>"> BLAST</a></li>
  			<li><a href="<c:url value="/srt.jsp"/>"> Sequence Retrieval</a></li>
        		<li><a href="#"> PubMed and Entrez</a></li>
        		<li><a href="${CGI_URL}/gbrowse/cryptodb"> GBrowse</a></li>
        		<li><a href="#"> CryptoCyc</a></li>
      		</ul>
		</li>
	</ul>
