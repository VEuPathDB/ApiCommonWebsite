<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<%@ attribute name="strategies"
              type="java.util.List"
              required="false"
              description="Currently active user object"
%>
 
<!-- begin of the html:form for rename query -->
<html:form method="get" action="/renameStrategy.do">
    <table border="0" cellpadding="5" cellspacing="0">
      <tr class="headerrow">
	<th>&nbsp;</th>
	<%-- <th>ID</th> --%>
        <th>&nbsp;</th>
        <th>&nbsp;</th>
        <th>Strategy</th>
        <th>&nbsp;</th>
        <th>&nbsp;</th>
        <th>Date</th>
        <th>Version</th>
        <th>Size</th>
      </tr>
      <c:set var="i" value="0"/>
      <%-- begin of forEach unsaved strategy in the category --%>
        <c:forEach items="${strategies}" var="strategy">
          <c:set var="strategyId" value="${strategy.strategyId}"/>
            <c:choose>
              <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
              <c:otherwise><tr class="linesalt"></c:otherwise>
            </c:choose>
            <td><input type=checkbox id="${strategyId}" onclick="updateSelectedList()"/></td>
            <%-- <td>${strategyId}</td> --%>
            <%-- need to see if this strategys id is in the session. --%>
            <c:set var="active" value=""/>
            <c:set var="activeStrategies" value="${sessionScope.wdkActiveStrategies}"/>
            <c:forEach items="${activeStrategies}" var="id">
              <c:if test="${strategyId == id}">
                <c:set var="active" value="true"/>
              </c:if>
            </c:forEach>
            <c:choose>
              <c:when test="${active == ''}">
                <td id="eye_${strategy.strategyId}" class="strat_inactive">
              </c:when>
              <c:otherwise>
                <td id="eye_${strategy.strategyId}" class="strat_active">
              </c:otherwise>
            </c:choose>
            <a href="javascript:void(0)" onclick="toggleEye(this,'${strategy.strategyId}')"><img src="/assets/images/transparent1.gif" alt="Toggle View of Strategy" /></a></td>
            <td>
              <img id="img_${strategyId}" class="plus-minus plus" src="/assets/images/sqr_bullet_plus.png" alt="" onclick="toggleSteps(${strategyId})"/>
            </td>
            <c:set var="dispNam" value="${strategy.name}"/>
            <td width=450>
              <div id="text_${strategyId}">
                <span onclick="enableRename('${strategyId}', '${strategy.name}')">${dispNam}</span>
              </div>
              <div id="name_${strategyId}" style="display:none"></div>          
            </td>
            <td>
              <div id="activate_${strategyId}">
                 <input type='button' value='Save As' onclick="enableRename('${strategyId}', '${strategy.name}')" />
              </div>       
              <div id="input_${strategyId}" style="display:none"></div>
            </td>
            <c:set var="stepId" value="${strategy.latestStep.stepId}"/>
            <td nowrap><input type='button' value='Download' onclick="downloadStep('${stepId}')" /><%--<a href="downloadStep.do?step_id=${stepId}">download</a>--%></td>
	    <td align='right' nowrap>${strategy.latestStep.lastRunTime}</td>
	    <td align='right' nowrap>
	    <c:choose>
              <c:when test="${strategy.latestStep.version == null || strategy.latestStep.version eq ''}">${wdkModel.version}</c:when>
              <c:otherwise>${strategy.latestStep.version}</c:otherwise>
            </c:choose>
            </td>
            <td align='right' nowrap>${strategy.latestStep.estimateSize}</td>
            <c:set value="${strategy.latestStep.answerValue.question.fullName}" var="qName" />
          </tr>
	  <!-- begin rowgroup for strategy steps -->
          <c:set var="j" value="0"/>
          <c:set var="steps" value="${strategy.allSteps}"/>
          <tbody id="steps_${strategyId}">
            <c:forEach items="${steps}" var="step">
            <c:choose>
              <c:when test="${i % 2 == 0}"><tr class="lines" style="display: none;"></c:when>
              <c:otherwise><tr class="linesalt" style="display: none;"></c:otherwise>
            </c:choose>
            <!-- offer a rename here too? -->
            <td colspan="3"></td>
            <c:choose>
              <c:when test="${j == 0}">
                <td nowrap><ul style="margin-left: 10px;"><li style="float:left;">Step ${j + 1} (${step.answerValue.resultSize}): ${step.customName}</li></ul></td>
              </c:when>
              <c:otherwise>
                <!-- only for boolean, need to check for transforms -->
                <c:choose>
                <c:when test="${j == 1}">
                  <td nowrap><ul style="margin-left: 10px;"><li style="float:left;">Step ${j + 1} (${step.answerValue.resultSize}): Step ${j}</li><li style="float:left;margin-top:-8px;" class="operation ${step.operation}" /><li style="float:left;">${step.childStep.customName}&nbsp;(${step.childStep.answerValue.resultSize})</li></ul></td>
                </c:when>
                <c:otherwise>
                  <td nowrap><ul style="margin-left: 10px; margin-top:-12px;"><li style="float:left;">Step ${j + 1} (${step.answerValue.resultSize}): Step ${j}</li><li style="float:left;margin-top:-8px;" class="operation ${step.operation}" /><li style="float:left;">${step.childStep.customName}&nbsp;(${step.childStep.answerValue.resultSize})</li></ul></td>
                </c:otherwise>
              </c:choose>
            </c:otherwise>
          </c:choose>
          <td colspan="5"/>
          <%-- <td></td>
            <td align="right" nowrap>
	      <c:choose>
	        <c:when test="${step.version == null || step.version eq ''}">${wdkModel.version}</c:when>
                <c:otherwise>${step.version}</c:otherwise>
              </c:choose>
            </td>
            <td align='right' nowrap>${step.answerValue.resultSize}</td>
              <c:set var="stepId" value="${step.stepId}"/>
              <td nowrap><a href="downloadStep.do?step_id=${stepId}">download</a></td> --%>
        </tr>
        <%-- <c:if test="${step.childStep != null}">
         <c:choose>
           <c:when test="${i % 2 == 0}"><tr class="lines" style="display:none;"></c:when>
           <c:otherwise><tr class="linesalt" style="display:none;"></c:otherwise>
         </c:choose>
         <td colspan="4"></td>
         <td nowrap>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${step.childStep.customName}</td>
         <!-- date? -->
         <td></td>
         <td align="right" nowrap>
           <c:choose>
	     <c:when test="${step.childStep.version == null || step.childStep.version eq ''}">${wdkModel.version}</c:when>
             <c:otherwise>${step.childStep.version}</c:otherwise>
           </c:choose>
         </td>
         <td align='right' nowrap>${step.childStep.estimateSize}</td>
           <c:set var="stepId" value="${step.childStep.stepId}"/>
         <td nowrap><a href="downloadStep.do?step_id=${stepId}">download</a></td>
        </c:if> --%>
        <c:set var="j" value="${j + 1}"/>
      </c:forEach>
    </tbody>
    <!-- end rowgroup for strategy steps -->
    <c:set var="i" value="${i+1}"/>
  </c:forEach>
  <!-- end of forEach strategy in the category -->
  </table>
  </html:form> 
  <!-- end of the html:form for rename query -->
