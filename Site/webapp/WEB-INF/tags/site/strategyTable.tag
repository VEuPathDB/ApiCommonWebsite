<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<%@ attribute name="model"
             type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
             required="false"
             description="Wdk Model Object for this site"
%>

<%@ attribute name="user"
              type="org.gusdb.wdk.model.jspwrap.UserBean"
              required="false"
              description="Currently active user object"
%>

<c:set var="strategies" value="${user.strategiesByCategory}"/>
<c:set var="modelName" value="${model.name}"/>
<c:set var="showOrthoLink" value="${fn:containsIgnoreCase(modelName, 'plasmodb') || fn:containsIgnoreCase(modelName, 'apidb') || fn:containsIgnoreCase(modelName, 'cryptodb')}" />
<c:set var="invalidStrategies" value="${user.invalidStrategies}" />


<!-- decide whether strategy is empty -->
<c:choose>
  <c:when test="${user == null || user.strategyCount == 0}">
  <div align="center">You have no searches in your history.  Please run a search from the <a href="/">home</a> page, or by using the "New Search" menu above, or by selecting a search from the <a href="/queries_tools.jsp">searches</a> page.</div>
  </c:when>
  <c:otherwise>
  <c:set var="typeC" value="0"/>
  <!-- begin creating tabs for history sections -->
  <ul id="history_tabs">
  <c:forEach items="${strategies}" var="strategyEntry">
  <c:set var="type" value="${strategyEntry.key}"/>
  <c:set var="isGeneRec" value="${fn:containsIgnoreCase(type, 'GeneRecordClass')}"/>
  <c:set var="histList" value="${strategyEntry.value}"/>
  <c:set var="recDispName" value="${histList[0].latestStep.answerValue.question.recordClass.type}"/>
  <c:set var="recTabName" value="${fn:substring(recDispName, 0, fn:indexOf(recDispName, ' ')-1)}"/>

  <c:set var="typeC" value="${typeC+1}"/>
  <c:choose>
    <c:when test="${typeC == 1}">
      <li id="selected">
    </c:when>
    <c:otherwise>
      <li>
    </c:otherwise>
  </c:choose>
  <a id="tab_${recTabName}" onclick="displayHist('${recTabName}')"
  href="javascript:void(0)">My&nbsp;${recDispName}&nbsp;Searches</a></li>
  </c:forEach>
  </ul>
<!-- should be a div instead of a table -->
<table class="clear_all">
   <tr>
      <td><a class="check_toggle" onclick="selectAllHist()" href="javascript:void(0)">select all</a>&nbsp|&nbsp;
          <a class="check_toggle" onclick="selectNoneHist()" href="javascript:void(0)">select none</a></td>
      <td></td>
      <td class="medium">
         <!-- display "delete button" -->
         <input type="button" value="Delete" onclick="deleteStrategies('deleteStrategy.do?strategy=')"/>
      </td>
   </tr>
</table>

  <c:set var="typeC" value="0"/>
  <!-- begin creating history sections to display strategies -->
  <c:forEach items="${strategies}" var="strategyEntry">
    <c:set var="type" value="${strategyEntry.key}"/>
    <c:set var="isGeneRec" value="${fn:containsIgnoreCase(type, 'GeneRecordClass')}"/>
    <c:set var="histList" value="${strategyEntry.value}"/>
    <c:set var="recDispName" value="${histList[0].latestStep.answerValue.question.recordClass.type}"/>
    <c:set var="recTabName" value="${fn:substring(recDispName, 0, fn:indexOf(recDispName, ' ')-1)}"/>

    <c:set var="typeC" value="${typeC+1}"/>
    <c:choose>
      <c:when test="${typeC == 1}">
        <div id="panel_${recTabName}" class="history_panel enabled">
      </c:when>
      <c:otherwise>
        <div id="panel_${recTabName}" class="history_panel">
      </c:otherwise> 
    </c:choose>

    
    <!-- begin of the html:form for rename query -->
    <html:form method="get" action="/renameStrategy.do">
       <table border="0" cellpadding="5" cellspacing="0">
          <tr class="headerrow">
	     <th>&nbsp;</th>
             <th>ID</th> 
             <th>icon</th>
             <th>&nbsp;</th>
             <th>Strategy</th>
             <th>&nbsp;</th>
             <th>Date</th>
             <th>Version</th>
             <th>Size</th>
             <th>&nbsp;</th>
          </tr>
          <c:set var="i" value="0"/>
          <!-- begin of forEach strategy in the category -->
          <c:forEach items="${histList}" var="strategy">
            <c:set var="strategyId" value="${strategy.strategyId}"/>
              <c:choose>
                <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
                <c:otherwise><tr class="linesalt"></c:otherwise>
              </c:choose>
              <td><input type=checkbox id="${strategyId}" onclick="updateSelectedList()"/></td>
              <td>${strategyId}</td>
              <td>&nbsp;</td>
              <td>
                 <img id="img_${strategyId}" class="plus-minus plus" src="/assets/images/sqr_bullet_plus.png" onclick="toggleSteps(${strategyId})"/>
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
                   <input type='button' value='Rename' onclick="enableRename('${strategyId}', '${strategy.name}')" />
                </div>       
                <div id="input_${strategyId}" style="display:none"></div>
              </td>
	      <td align='right' nowrap>${strategy.latestStep.lastRunTime}</td>
	      <td align='right' nowrap>
	      <c:choose>
	        <c:when test="${strategy.latestStep.version == null || strategy.latestStep.version eq ''}">${wdkModel.version}</c:when>
                <c:otherwise>${strategy.latestStep.version}</c:otherwise>
              </c:choose>
              </td>
              <td align='right' nowrap>${strategy.latestStep.estimateSize}</td>
              <c:set value="${strategy.latestStep.answerValue.question.fullName}" var="qName" />
              <c:set var="stepId" value="${strategy.latestStep.stepId}"/>
              <td nowrap><a href="downloadUserAnswer.do?user_answer_id=${stepId}">download</a></td>
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
                  <td colspan="4"></td>
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
                  <td nowrap><a href="downloadUserAnswer.do?user_answer_id=${stepId}">download</a></td> --%>
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
                  <td nowrap><a href="downloadUserAnswer.do?user_answer_id=${stepId}">download</a></td>
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
</div>
</c:forEach>
<!-- end of showing strategies grouped by RecordTypes -->

<c:if test="${typeC != 1}"><hr></c:if>

<table>
   <tr>
      <td><a class="check_toggle" onclick="selectAllHist()" href="javascript:void(0)">select all</a>&nbsp|&nbsp;
          <a class="check_toggle" onclick="selectNoneHist()" href="javascript:void(0)">select none</a></td>
      <td class="medium">
         <!-- display "delete button" -->
         <input type="button" value="Delete" onclick="deleteStrategies('deleteStrategy.do?strategy=')"/>
      </td>
   </tr>
   <%-- <tr>
      <td>
         <!-- display helper information -->
         <font class="medium"><b>Understanding AND, OR and NOT</b>:</font>
         <table border='0' cellspacing='3' cellpadding='0'>
            <tr>
               <td width='100'><font class="medium"><b>1 and 2</b></font></td>
               <td><font class="medium">Genes that 1 and 2 have in common. You can also use "1 intersect 2".</font></td>
            </tr>
            <tr>
               <td width='100'><font class="medium"><b>1 or 2</b></font></td>
               <td><font class="medium">Genes present in 1 or 2, or both. You can also use "1 union 2".</font></td>
            </tr>
            <tr>
               <td width='100'><font class="medium"><b>1 not 2</b></font></td>
               <td><font class="medium">Genes in 1 but not in 2. You can also use "1 minus 2".</font></td>
            </tr>
         </table>
      </td>
   </tr> --%>
</table>


  </c:otherwise>
</c:choose> 
<!-- end of deciding strategy emptiness -->

<%--  </td>
  <td valign=top class=dottedLeftBorder></td> 
</tr>
</table> --%>
