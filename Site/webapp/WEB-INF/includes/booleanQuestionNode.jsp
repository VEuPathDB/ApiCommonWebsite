<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>

<dir>

  <nested:define id="rootClassName" property="class.name"/>
  <c:choose>
    <c:when test="${rootClassName eq 'org.gusdb.wdk.model.jspwrap.BooleanQuestionNodeBean'}">

    <img alt="spacer" width="12" height="16" src="<c:url value="/images/booleanBullet.gif" />">
    <nested:write property="operation"/><br>

    <nested:nest property="firstChild">
      <jsp:include page="/WEB-INF/includes/booleanQuestionNode.jsp"/>
    </nested:nest>

    <nested:nest property="secondChild">
      <jsp:include page="/WEB-INF/includes/booleanQuestionNode.jsp"/>
    </nested:nest>

    </c:when>	
    <c:otherwise>

      <nested:define id="wdkQ" property="question"/>
      <nested:define id="leafPref" property="leafId"/>
      <c:set value="${leafPref}_" var="leafPrefix"/>
      <c:set value="${wdkQ.params}" var="qParams"/>

      <!-- show all params of a question, collect help info along the way -->
      <c:set value="Help for question: ${wdkQ.displayName}" var="fromAnchorQ"/>
      <jsp:useBean id="helpQ" class="java.util.HashMap"/>

      <!-- put an anchor here for linking back from help sections -->
      <A name="${fromAnchorQ}"></A>
         <table>
            <!-- Print out question -->
            <!-- display description -->
            <tr><td colspan="2">
                <img alt="spacer" width="12" height="16" src="<c:url value="/images/booleanBullet.gif" />">
                <b><jsp:getProperty name="wdkQ" property="displayName"/></b></td></tr>

            <!-- display params -->
            <c:forEach items="${qParams}" var="qP">
               <!-- an individual param (can not use fullName, w/ '.', for mapped props) -->
               <c:set value="${qP.name}" var="pNam"/>
               <tr><td align="right"><b><jsp:getProperty name="qP" property="prompt"/></b></td>

               <!-- choose between flatVocabParam and straight text or number param -->
               <td>
               <c:choose>
                  <c:when test="${qP.class.name eq 'org.gusdb.wdk.model.jspwrap.FlatVocabParamBean'}">
                  <c:set var="opt" value="0"/>
                    <c:choose>
                      <c:when test="${qP.multiPick}">
                        <!-- multiPick is true, use scroll pane -->
                        <html:select  property="myMultiProp(${leafPrefix}${pNam})" multiple="1">
                          <c:set var="opt" value="${opt+1}"/>
                          <c:set var="sel" value=""/>
                          <c:if test="${opt == 1}"><c:set var="sel" value="selected"/></c:if>      
                          <html:options property="values(${leafPrefix}${pNam})" labelProperty="labels(${leafPrefix}${pNam})"/>
                        </html:select>
                      </c:when> 
                      <c:otherwise>
                        <!-- multiPick is false, use pull down menu -->
                        <html:select  property="myProp(${leafPrefix}${pNam})">
                          <c:set var="opt" value="${opt+1}"/>
                          <c:set var="sel" value=""/>
                          <c:if test="${opt == 1}"><c:set var="sel" value="selected"/></c:if>      
                          <html:options property="values(${leafPrefix}${pNam})" labelProperty="labels(${leafPrefix}${pNam})"/>
                        </html:select>
                      </c:otherwise>
                    </c:choose>
                  </c:when>
                  <c:otherwise>
                      <html:text property="myProp(${leafPrefix}${pNam})"/>
                  </c:otherwise>
                </c:choose>

                <html:errors property="myProp(${leafPrefix}${pNam})"/>
                
                <c:set var="anchorQp" value="HELP_${fromAnchorQ}_${pNam}"/>
                <c:set target="${helpQ}" property="${anchorQp}" value="${qP}"/>
                   &nbsp;&nbsp;&nbsp;&nbsp;
                   <a href="#${anchorQp}">
                   <img src="/assets/images/help.png" border="0" alt="Help!"></a>
                </td>
                </tr>
            </c:forEach>
            <c:set target="${helps}" property="${fromAnchorQ}" value="${helpQ}"/>  

            <!-- display boolean stuff -->
            <tr>
               <!-- get boolean operations and display in select box -->
	       <td align="right">
	          <html:select property="myProp(${leafPrefix}_nextBooleanOperation)">
                     <c:set value="booleanOps" var="booleanName"/>
                     <html:options property="values(${booleanName})" labelProperty="labels(${booleanName})"/>
                  </html:select>
               </td>
               <!-- get possible questions to boolean with and display them -->
               <td>
                  <html:select property="myProp(${leafPrefix}_nextQuestionOperand)">
                     <c:set var="recordClass" value="${wdkQ.recordClass}"/>
                     <c:set var="questions" value="${recordClass.questions}"/>
                     <c:forEach items="${questions}" var="q">
                        <c:set value="${q.fullName}" var="qFullName"/>
                        <c:set value="${q.displayName}" var="qDispName"/>
                        <html:option value="${qFullName}">${qDispName}</html:option>
                     </c:forEach>
                  </html:select>
                <html:submit property="process_boolean_question" value="Expand (${leafPref})"/>  
             </td></tr>
         </table>

    </c:otherwise>
  </c:choose>

</dir>
