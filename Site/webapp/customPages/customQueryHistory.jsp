<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<!-- get wdkUser saved in session scope -->
<c:set var="wdkUser" value="${sessionScope.wdkUser}"/>
<c:set var="wdkModel" value="${applicationScope.wdkModel}"/>

<c:set var="dsCol" value="${param.dataset_column}"/>
<c:set var="dsColVal" value="${param.dataset_column_label}"/>

<site:home_header refer="customQueryHistory" />
<site:menubar />

<script type="text/javascript">
<!--

currentStrategyId = 0;

function enableRename(histId, name) {
   // close the previous one
   disableRename();
   
   currentStrategyId = histId;
   var button = document.getElementById('activate_' + histId);
   button.style.display = 'none';
   var text = document.getElementById('text_' + histId);
   text.style.display = 'none';
   var nameBox = document.getElementById('name_' + histId);
   nameBox.innerHTML = "<input name='strategy' type='hidden' value='" + histId + "'>"
                  + "<input id='name' name='name' type='text' size='42' maxLength='2000' value='" + name + "' style='margin-right:4px;'>" 
   nameBox.style.display='block';
   var input = document.getElementById('input_' + histId);
   input.innerHTML = "<input type='submit' value='Update'>"
                   + "<input type='reset' value='Cancel' onclick='disableRename()'>";
   input.style.display='block';
   nameBox = document.getElementById('name');
   nameBox.select();
   nameBox.focus();
}

function disableRename() {
   if (currentStrategyId && currentStrategyId != '0') {
      var button = document.getElementById('activate_' + currentStrategyId);
      button.style.display = 'block';
      var name = document.getElementById('name_' + currentStrategyId);
      name.innerText = '';
      name.style.display = 'none';
      var input = document.getElementById('input_' + currentStrategyId);
      input.innerText = '';
      input.style.display = 'none';
      var text = document.getElementById('text_' + currentStrategyId);
      text.style.display = 'block';
      currentStrategyId = 0;
   }
}

//-->
</script>

<div id="contentwrapper">
    <div id="contentcolumn2">
        <div class="innertube">

<div id="search_history">
<site:strategyTable model="${wdkModel}" user="${wdkUser}" />
</div>


        </div>
    </div>
</div>

<site:footer/>
