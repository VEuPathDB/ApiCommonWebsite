<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="first_step"
	      type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Protocol from the SummaryPage"
%>
<%@ attribute name="stratId"
              required="true"
              description="Protocol from the SummaryPage"
%>			
<%@ attribute name="stratName"
			  required="true"
			  description="Protocol from the SummaryPage"
%>
<strategy name="${stratName}" id="${stratId}">
	<c:forEach var="step" items="${first_step.allSteps}">
		<c:set value="${step.stepId}" var="id" />
		<c:set value="${step.customName}" var="customName" />
		<c:set value="${step.isCollapsible}" var="collapsible" />
		<c:set value="${step.collapsedName}" var="collapsedName" />
		<c:set value="${step.shortDisplayName}" var="sDName" />
		<c:set value="${step.customName}" var="cName" />
		<c:set value="${step.dataType}" var="dataType" />
		<c:set value="${step.resultSize}" var="resultSize" />
		<c:set value="${step.answerValue.question.fullName}" var="questionName" />
		<c:set value="${step.answerValue.question.displayName}" var="displayName"/>
		<c:set value="${step.answerValue.internalParams}" var="params"/>
		<c:set value="${step.answerValue.question.paramsMap}" var="displayParams"/>
		<c:set value="${step.answerValue.questionUrlParams}" var="urlParams"/>
		<c:set value="${step.isBoolean}" var="isboolean" />
		<c:if test="${isboolean}">
		<c:set value="${step.childStep.stepId}" var="child_id" />
		<c:set value="${step.childStep.customName}" var="child_customName" />
		<c:set value="${step.childStep.isCollapsible}" var="child_collapsible" />
		<c:set value="${step.childStep.collapsedName}" var="child_collapsedName" />
		<c:set value="${step.childStep.shortDisplayName}" var="child_sDName" />
		<c:set value="${step.childStep.customName}" var="child_cName" />
		<c:set value="${step.childStep.dataType}" var="child_dataType" />
		<c:set value="${step.childStep.resultSize}" var="child_resultSize" />
		<c:set value="${step.childStep.answerValue.question.fullName}" var="child_questionName" />
		<c:set value="${step.childStep.answerValue.question.displayName}" var="child_displayName"/>
		<c:set value="${step.childStep.answerValue.internalParams}" var="params"/>
		<c:set value="${step.childStep.answerValue.question.paramsMap}" var="displayParams"/>
		<c:set value="${step.childStep.answerValue.questionUrlParams}" var="urlParams"/>
		<c:set value="${step.childStep.isBoolean}" var="child_isboolean" />
		</c:if>
		<%--	<c:choose>
				<c:when test="${collapsible == true}">
					<c:choose>
						<c:when test="${step.isFirstStep}">
							<site:xml_strat first-step="${step}" stratName="${collapsedName}" stratId="${stratId}_${id}" />
						</c:when>
						<c:otherwise>
							<site:xml_strat first-step="${step.childStep}" stratName="${collapsedName}" stratId="${stratId}_${id}" />
						</c:otherwise>
					</c:choose>
					<site:xml_strat first-step="{step}">
				</c:when>
				<c:otherwise>--%>
				<c:choose>
					<c:when test="${isboolean}">
						<step name="${cName}"
							 id="${id}"
							 isCollapsed="${collapsible}"
							 dataType="${dataType}"
							 shortName="${sDName}"
							 results="${resultSize}"
							 questionName="${questionName}"
							 displayName="${displayName}"
							 isboolean="${isboolean}"
							 operation="${step.operation}">
							
							<step name="${child_cName}"
								 customName="${child_customName}"
								 id="${child_id}"
								 isCollapsed="${child_collapsible}"
								 dataType="${child_dataType}"
								 shortName="${child_sDName}"
								 results="${child_resultSize}"
								 questionName="${child_questionName}"
								 displayName="${child_displayName}"
								 isboolean="${child_isboolean}">
								<params>
									<c:forEach items="${displayParams}" var="p">
				                        <c:set var="pNam" value="${p.key}"/>
				                        <c:set var="qP" value="${p.value}"/>
										<c:set var="aP" value="${params[pNam]}"/>
										<jsp:setProperty name="qP" property="paramValue" value="${aP}" />
			                            <jsp:setProperty name="qP" property="truncateLength" value="1000" />
										<param name="${pNam}" prompt="${fn:escapeXml(qP.prompt)}" value="${qP.decompressedValue}" className="${qP.class.name}"/>
									</c:forEach>
								</params>
							</step>
					</c:when>
					<c:otherwise>
						<step name="${cName}"
							 customName="${customName}"
							 id="${id}"
							 isCollapsed="${collapsible}"
							 dataType="${dataType}"
							 shortName="${sDName}"
							 results="${resultSize}"
							 questionName="${questionName}"
							 displayName="${displayName}"
							 isboolean="${isboolean}">
							<params>
								<urlParams><![CDATA[${fn:escapeXml(urlParams)}]]></urlParams>
								<c:forEach items="${displayParams}" var="p">
			                        <c:set var="pNam" value="${p.key}"/>
			                        <c:set var="qP" value="${p.value}"/>
									<c:set var="aP" value="${params[pNam]}"/>
									<jsp:setProperty name="qP" property="paramValue" value="${aP}" />
		                            <jsp:setProperty name="qP" property="truncateLength" value="1000" />
									<param name="${pNam}" prompt="${qP.prompt}" value="${qP.decompressedValue}" className="${qP.class.name}"/>
								</c:forEach>
							</params>
					</c:otherwise>
				</c:choose>
			<%--	</c:otherwise>
			</c:choose>--%>
		</step>
	</c:forEach>
</strategy>