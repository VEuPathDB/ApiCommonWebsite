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

<c:set var="strategies" value="${user.userStrategiesByCategory}"/>
<c:set var="modelName" value="${model.name}"/>
<c:set var="showOrthoLink" value="${fn:containsIgnoreCase(modelName, 'plasmodb') || fn:containsIgnoreCase(modelName, 'apidb') || fn:containsIgnoreCase(modelName, 'cryptodb')}" />
<c:set var="invalidStrategies" value="${user.invalidUserStrategies}" />

<h1>My Searches</h1>

<!-- decide whether strategy is empty -->
<c:choose>
  <c:when test="${user == null || user.strategyCount == 0}">
  <span align="center">You have no searches in your history.  Please run a search from the <a href="/">home</a> page, or by using the "New Search" menu above, or by selecting a search from the <a href="/queries_tools.jsp">searches</a> page.</span>
  </c:when>
  <c:otherwise>
  <c:set var="typeC" value="0"/>
  <!-- begin creating tabs for history sections -->
  <ul id="history_tabs">
  <c:forEach items="${strategies}" var="strategyEntry">
  <c:set var="type" value="${strategyEntry.key}"/>
  <c:set var="isGeneRec" value="${fn:containsIgnoreCase(type, 'GeneRecordClass')}"/>
  <c:set var="histList" value="${strategyEntry.value}"/>
  <c:set var="recDispName" value="${histList[0].latestStep.filterUserAnswer.recordPage.question.recordClass.type}"/>
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

  <c:set var="typeC" value="0"/>
  <!-- begin creating history sections to display strategies -->
  <a class="check_toggle" onclick="selectAllHist()" href="javascript:void(0)">select all</a>&nbsp|&nbsp;
  <a class="check_toggle" onclick="selectNoneHist()" href="javascript:void(0)">select none</a>
  <c:forEach items="${strategies}" var="strategyEntry">
    <c:set var="type" value="${strategyEntry.key}"/>
    <c:set var="isGeneRec" value="${fn:containsIgnoreCase(type, 'GeneRecordClass')}"/>
    <c:set var="histList" value="${strategyEntry.value}"/>
    <c:set var="recDispName" value="${histList[0].latestStep.filterUserAnswer.recordPage.question.recordClass.type}"/>
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
             <th>&nbsp;</th>
             <th>ID</th> 
             <th>&nbsp;</th>
             <th>Strategy</th>
             <th>Date</th>
             <th>Version</th>
             <th>Size</th>
             <%-- <c:if test="${isGeneRec}"><th>&nbsp;</th></c:if> --%>
             <th>&nbsp;</th>
             <th>&nbsp;</th>
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
              <td>
                 <img id="img_${strategyId}" class="plus-minus plus" src="/assets/images/sqr_bullet_plus.png" onclick="toggleSteps(${strategyId})"/>
              </td>
              <td><input type=checkbox id="${strategyId}" onclick="updateSelectedList()"/></td>
              <td>${strategyId}</td>
              <td nowrap>
                <%-- <input type='button' id="btn_${strategyId}" value='Rename'
                 onclick="enableRename('${strategyId}', '${strategy.name}')" /> --%>
              </td>
              <c:set var="dispNam" value="${strategy.name}"/>
              <td width=450>
                <div id="text_${strategyId}">
                  <span onclick="enableRename('${strategyId}', '${strategy.name}')">${dispNam}</span>
                  <input type='button' value='Rename' onclick="enableRename('${strategyId}', '${strategy.name}')" />
                </div>          
                <div id="input_${strategyId}" style="display:none"></div>
              </td>
	      <td align='right' nowrap>${strategy.latestStep.filterUserAnswer.lastRunTime}</td>
	      <td align='right' nowrap>
	      <c:choose>
	        <c:when test="${strategy.latestStep.filterUserAnswer.version == null || strategy.latestStep.filterUserAnswer.version eq ''}">N/A</c:when>
                <c:otherwise>${strategy.latestStep.filterUserAnswer.version}</c:otherwise>
              </c:choose>
              </td>
              <td align='right' nowrap>${strategy.latestStep.filterUserAnswer.estimateSize}</td>
              <c:set value="${strategy.latestStep.filterUserAnswer.recordPage.question.fullName}" var="qName" />
              <td nowrap>
              <c:set var="surlParams">
                showSummary.do?strategy=${strategyId}
              </c:set>
              <a href="${surlParams}">view</a>
              </td>
              <td nowrap><a href="downloadStrategyAnswer.do?strategy=${strategyId}">download</a></td>
              <%-- disabled;  what does it do now, w/ strategies?
              <c:if test="${isGeneRec && showOrthoLink}">
              <td nowrap>
              <c:set var="dsColUrl" 
                     value="showQuestion.do?questionFullName=InternalQuestions.GenesByOrthologs&strategyId=${user.signature}:${strategyId}&questionSubmit=Get+Answer&goto_summary=0"/>
                <a href='<c:url value="${dsColUrl}"/>'>orthologs</a>
              </td>	    
              </c:if>
              --%>
            </tr>
	    <!-- begin rowgroup for strategy steps -->
	    <c:set var="j" value="0"/>
            <c:set var="steps" value="${strategy.allSteps}"/>
            <tbody id="steps_${strategyId}">
               <c:forEach items="${steps}" var="step">
               <c:choose>
                 <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
                 <c:otherwise><tr class="linesalt"></c:otherwise>
               </c:choose>
                  <!-- offer a rename here too? -->
                  <td colspan="4"></td>
                  <td nowrap>Step ${j + 1}: ${step.customName}</td>
                  <!-- date? -->
                  <td></td>
                  <td align="right" nowrap>
	          <c:choose>
	            <c:when test="${step.filterUserAnswer.version == null || step.filterUserAnswer.version eq ''}">N/A</c:when>
                    <c:otherwise>${step.filterUserAnswer.version}</c:otherwise>
                    </c:choose>
                  </td>
                  <td align='right' nowrap>${step.filterUserAnswer.estimateSize}</td>
                  <td nowrap>
                  <c:set var="surlParams">
                    showSummary.do?strategy=${strategyId}&step=${j}
                  </c:set>
                     <a href="${surlParams}">view</a>
                  </td>
                  <td nowrap><a href="downloadStrategyAnswer.do?strategy=${strategyId}&step=${j}">download</a></td>
                  <%-- disabled; do we offer this anymore?  what happens?  is it a new strategy?
                  <c:if test="${isGeneRec && showOrthoLink}">
                  <td nowrap>
                  <c:set var="dsColUrl" 
                     value="showQuestion.do?questionFullName=InternalQuestions.GenesByOrthologs&strategyId=${user.signature}:${strategyId}&questionSubmit=Get+Answer&goto_summary=0"/>
                    <a href='<c:url value="${dsColUrl}"/>'>orthologs</a>
                  </td>	    
                  </c:if>
                  --%>
               </tr>
               <%-- display subquery info if exists?
               <c:if test="${step.childStep != null}">
               <c:choose>
                 <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
                 <c:otherwise><tr class="linesalt"></c:otherwise>
               </c:choose>                  <td colspan="4"></td>
                  <td nowrap>Step ${j + 1}: ${step.childStepUserAnswer.customName}</td>
                  <!-- date? -->
                  <td></td>
                  <td align="right" nowrap>
	          <c:choose>
	            <c:when test="${step.filterUserAnswer.version == null || step.filterUserAnswer.version eq ''}">N/A</c:when>
                    <c:otherwise>${step.filterUserAnswer.version}</c:otherwise>
                    </c:choose>
                  </td>
                  <td align='right' nowrap>${step.filterUserAnswer.estimateSize}</td>
                  <td nowrap>
                  <c:set var="surlParams">
                    showSummary.do?strategy=${strategyId}&step=${j}
                  </c:set>
                     <a href="${surlParams}">view</a>
                  </td>
                  <td nowrap><a href="downloadStrategyAnswer.do?strategy=${strategyId}&step=${j}">download</a></td>
               </c:if>
               --%>
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
<!-- end of showing user answers grouped by RecordTypes -->

<c:if test="${typeC != 1}"><hr></c:if>

<table>
   <tr>
      <td class="medium">
         <div>&nbsp;</div>
         <!-- display "delete button" -->
         <input type="button" value="Delete Selected" onclick="deleteStrategies('deleteStrategy.do?strategy=')"/>
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

<%-- incorrect anyway, will deal w/ this when we need it
<!-- display invalid strategy list -->
<c:set var="invalidStrategies" value="${user.invalidUserStrategies}" />
<c:if test="${fn:length(invalidStrategies) > 0}">

    <hr>

    <a name="incompatible"></a><h3>Incompatible Queries</h3>

    <p>This section lists your queries from previous versions of ${model.displayName} that
        are no longer compatible with the current version of ${model.displayName}.  In most
        cases, you will be able to work around the incompatibility by finding an
        equivalent query in this version, and running it with similar parameter
        values.</p>
    <p>If you have problems <a href="<c:url value="help.jsp" />">drop us a line</a>.</p>

    <table>

        <tr class="headerrow">
            <th>ID</th> 
            <th>Query</th>
            <th>Size</th>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
        </tr>

        <c:forEach items="${invalidStrategies}" var="strategy">
            <tr>
                <c:set var="strategyId" value="${strategy.strategyId}"/>

                <c:choose>
                    <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
                    <c:otherwise><tr class="linesalt"></c:otherwise>
                </c:choose>

                <td class="medium">${strategyId}
	               <!-- begin of floating info box -->
                   <div id="div_${strategyId}" 
	                    class="medium"
                        style="display:none;font-size:8pt;width:610px;position:absolute;left:0;top:0;">
                       <table cellpadding="2" cellspacing="0" border="0"bgcolor="#ffffCC">
                           <tr>
                              <td valign="top" align="right" width="10" class="medium" nowrap><b>Query&nbsp;:</b></td>
                              <td valign="top" align="left" class="medium">${strategy.name}</td>
                           </tr>

                           <c:set var="params" value="${strategy.params}"/>
                           <c:set var="paramNames" value="${strategy.paramNames}"/>
                           <c:forEach items="${params}" var="item">
                               <c:set var="pName" value="${item.key}"/>
                               <tr>
                                  <td align="right" valign="top" class="medium" nowrap><i>${paramNames[pName]}</i> : </td>
                                  <td class="medium">${item.value}</td>
                               </tr>
                           </c:forEach>
                     </table>
                   </div> 
	               <!-- end of floating info box -->
                </td>
                <c:set var="dispNam" value="${strategy.name}"/>
                <td>
                    <div id="text_${strategyId}">${dispNam}</div>
                    <div id="input_${strategyId}" style="display:none"></div>
                </td>
                <td align='right' nowrap>${strategy.estimateSize}</td>

                <td nowrap>
                    <c:set var="surlParams" value="showSummary.do?strategy=${strategyId}" />
                    <a href="${surlParams}">show</a>
                </td>

                <td nowrap>
                    <a href="deleteStrategy.do?strategy=${strategyId}"
                       title="delete saved query #${strategyId}"
                       onclick="return deleteStrategy('${strategyId}', '${strategy.name}');">delete</a>
                </td>

            </tr>
            <c:set var="i" value="${i+1}"/>
        </c:forEach>

        <!-- delete all invalid strategies -->
        <tr>
          <td colspan="5" class="medium">
             <div>&nbsp;</div>
             <!-- display delete all invalid strategies button -->
             <input type="button" value="Delete All Incompatible Queries" onclick="deleteAllInvStrats()"/>
          </td>
       </tr>
    </table>
</c:if>

<!-- end of display invalid strategy list -->
--%>

  </td>
  <td valign=top class=dottedLeftBorder></td> 
</tr>
</table> 
