<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="project" value="${applicationScope.wdkModel.name}" />
<c:set var="xqSetMap" value="${wdkModel.xmlQuestionSetsMap}"/>
<c:set var="xqSet" value="${xqSetMap['XmlQuestions']}"/>
<c:set var="xqMap" value="${xqSet.questionsMap}"/>
<c:set var="newsQuestion" value="${xqMap['News']}"/>
<c:set var="newsAnswer" value="${newsQuestion.fullAnswer}"/>
<c:set var="dateStringPattern" value="dd MMMM yyyy HH:mm"/>

<div id="leftcolumn">
	<div class="innertube">
		<div id="menu_lefttop">
				<img src="/assets/images/TriTrypDB/menu_lft1.png" alt="" width="208" height="12" />
				<a class="heading" href="#">News</a>
				<div class="menu_lefttop_drop"><ul>
                    <c:choose>
                      <c:when test="${newsAnswer.resultSize < 1}">
                        No news now, please check back later.<br>
                      </c:when>
                      <c:otherwise>
                        <c:set var="i" value="1"/>
                        <ul>
                        <c:forEach items="${newsAnswer.recordInstances}" var="record">
                        <c:if test="${i <= 4}">
                          <c:set var="attrs" value="${record.attributesMap}"/>
                          
                          <fmt:parseDate pattern="${dateStringPattern}" 
                                         var="pdate" value="${attrs['date']}"/> 
                          <fmt:formatDate var="fdate" value="${pdate}" pattern="d MMMM yyyy"/>
                    
                          <li><b>${fdate}</b>
                                 <a href="<c:url value="/showXmlDataContent.do?name=XmlQuestions.News#newsItem${i}"/>">
                                   ${attrs['headline']}
                                 </a></li>
                        </c:if>
                        <c:set var="i" value="${i+1}"/>
                        </c:forEach>
                        <li>
                          <a href="<c:url value="/showXmlDataContent.do?name=XmlQuestions.News"/>"
                             class="blue">All ${project} News</a>
                        </li>
                        </ul>
                      </c:otherwise>
                    </c:choose>
				</ul></div>
				<img src="/assets/images/TriTrypDB/menu_lft1.png" alt="" width="208" height="12" />
				<a class="heading" href="#">Web Tutorials</a>
				<div class="menu_lefttop_drop"><ul>
						<li>Introduction<br />(<a href="#">Quick Time</a>) (<a href="#">Windows Media</a>) (<a href="#">Flash</a>)</li>
						<li>Queries and Tools<br />(<a href="#">Quick Time</a>) (<a href="#">Windows Media</a>) (<a href="#">Flash</a>)</li>
						<li>Query History<br />(<a href="#">Quick Time</a>) (<a href="#">Windows Media</a>) (<a href="#">Flash</a>)</li>
						<li>Introduction to the Genome Browser<br />(<a href="#">Quick Time</a>) (<a href="#">Windows Media</a>) (<a href="#">Flash</a>)</li>
						<li>List of Gene Identifiers as Query Input<br />(<a href="#">Quick Time</a>) (<a href="#">Windows Media</a>) (<a href="#">Flash</a>)</li>
				</ul></div>
				<img src="/assets/images/TriTrypDB/menu_lft1.png" alt="" width="208" height="12" />
				<a class="heading" href="#">Community Links</a>
				<div class="menu_lefttop_drop"><ul>
						<li><a href="#"> EuPathDB.org</a></li>
						<li><a href="#">PlasmoDB.org</a></li>
						<li><a href="#">ToxoDB.org</a></li>
						<li><a href="#">ApiEST-DB</a></li>
						<li><a href="#"> C. parvum Genomic DNA Sequence Demonstration Project at Univ. of Minnesota</a></li>
						<li><a href="#">C. parvum Genome Mapping Project at Cambridge</a></li>
						<li><a href="#">University of Oklahoma's Advanced Center for Genome Technology</a></li>
						<li><a href="#">Stanford Human Genome Center</a></li>
						<li><a href="#">Stanford Genome Technology Center</a></li>
						<li><a href="#">Whitehead Institute for Biomedical Research/MIT Center for Genome Research</a></li>
						<li><a href="#">Baylor College of Medicine Human Genome Sequencing Center</a></li>
						<li><a href="#">The Institute for Genomic Research</a></li>
						<li><a href="#">Oak Ridge National Laboratory Section of Computational Biology</a></li>
						<li><a href="#">The Sanger Centre</a></li>
						<li><a href="#">DOE Joint Genome Institute</a></li>
						<li><a href="#">The University of Washington Genome Center</a></li>
						<li><a href="#">Washington University in St. Louis Genome Sequencing Center</a></li>
						<li><a href="#">MCV-VCU Massey Cancer Center Nucleic Acids Research Facility</a></li>
						<li><a href="#">National Center for Biotechnology Information</a></li>
						<li><a href="#"> Minnesota TriTrypDBsporidium parvum Genome Project</a></li>
						<li><a href="#">TriTrypDBsporidium hominis Research at the Virginia Commonwealth University Center for the Study of Biologial Complexity </a></li>
						<li><a href="#"> VBI PathInfo</a></li>
						<li><a href="#">Chartered Institute of Environmental Health </a></li>
				</ul></div>
				<img src="/assets/images/TriTrypDB/menu_lft1.png" alt="" width="208" height="12" />
				<a class="heading" href="#">Information and Help</a>
				<div class="menu_lefttop_drop"><ul>
						<li> <a href="#">Glossary of Terms</a></li>
						<li><a href="#">Website Usage Statistics</a></li>
						<li><a href="#">Acknowledgements</a></li>
						<li><a href="#">Contact Us</a></li>
				</ul></div>
		</div>
	</div>
</div>
</body>

</html>	
	
