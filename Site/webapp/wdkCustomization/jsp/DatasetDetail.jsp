<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- get wdkXmlQuestionSets saved in request scope --%>
<c:set var="wdkModel" value="${applicationScope.wdkModel}" />
<c:set var="datasets" value="${requestScope.datasets}"/>
<c:set var="question" value="${requestScope.question}" />
<c:set var="recordClass" value="${requestScope.recordClass}" />

<imp:pageFrame banner="Data Sets" refer="data-set" >

  <script>
    !function($) {
      // prevent FOUC
      $('#data-sets').css('opacity', 0);
      $('html').css('overflow', 'hidden');
      $(function() {
        $('#data-sets') .css('opacity', 1);
        $('html').css('overflow', '');
      });
    }(jQuery);
  </script>

  <style>
    h1 {
      text-align: left;
    }
    h3 {
      margin: .4em 0;
      font-size: 1em;
    }
    h3 + div {
      margin-bottom: 1em;
    }
    #data-sets {
      padding: 6px;
      /*padding-left: 20em;*/
    }
    .toggle-section.ui-accordion .ui-accordion-header {
      font-size: 1.2em;
    }
    .toggle-section.ui-accordion .ui-accordion-icons {
      padding-left: 1.8em;
    }
    .toggle-section.ui-accordion .ui-accordion-header.ui-state-active {
      background-color: white;
      border-bottom: none;
    }
    #data-sets > .toggle-section.ui-accordion {
      margin: 6px 0;
    }
    #data-sets > .toggle-section.ui-accordion > .ui-accordion-header {
      font-size: 150%;
      background-color: #dfdfdf;
      padding-left: 1.6em;
    }
    #data-sets .ui-accordion-content {
      padding-top: 2px;
      padding-bottom: 0;
    }
    #data-sets > .toggle-section.ui-accordion > .ui-accordion-content {
      padding: 0 2px;
    }
    #data-sets > .toggle-section.ui-accordion > .ui-accordion-content h3 {
      text-transform: capitalize;
    }
    .toggle-section.ui-accordion .ui-accordion-header {
      padding-left: 2em;
    }
    .toggle-section.ui-accordion .ui-accordion-content {
      padding-left: 3em;
    }
    .toggle-section.ui-accordion .ui-accordion-header .ui-accordion-header-icon {
      top: 45%;
    }
    .toggle-section ul {
      font-size: 1em;
      margin-left: 1em;
    }
    .toggle-section-link {
      display: none;
    }
  </style>


<%-- show all xml question sets --%>
<div id="data-sets">
  <a name="_top"></a>
  <h1>Data Sets</h1>

    <div class="smallitalics">
      (Click on a category to jump to the corresponding section in the page.
      To search for text on the page, first click
      <a class="wdk-toggle-group" href="#" data-container="#data-sets" data-show="true">Expand all</a>
      to make all details visible for searching.)
    </div> <br/>

    <ul id="toc">
      <c:forEach items="${datasets}" var="category">
        <li><a href="#${category.key}"><i>${category.key}</i></a></li>
      </c:forEach>
    </ul>

  <div class="record-toolbar ui-widget ui-helper-clearfix">
    <a href="#show-all">
      Expand all<span class="ui-icon ui-icon-arrowthickstop-1-s"></span>
    </a>
    <a href="#hide-all">
      Collapse all<span class="ui-icon ui-icon-arrowthickstop-1-n"></span>
    </a>
  </div>

  <c:forEach items="${datasets}" var="category">
  <imp:toggle name="${category.key}" displayName="${category.key}" isOpen="true">
    <jsp:attribute name="content">
        <c:forEach items="${category.value}" var="record">
          <c:set var="wdkRecord" value="${record}" scope="request" />
          <c:set var="primaryKey" value="${record.primaryKey}"/>
          <c:set var="attributes" value="${record.attributes}"/>
          <c:set var="datasetId" value="${attributes['dataset_id']}" />
          <c:set var="name" value="${attributes['dataset_name']}" />
          <c:set var="displayName" value="${attributes['display_name']}" />
          <c:set var="categories" value="${attributes['category']}" />
          <c:set var="organism" value="${attributes['organisms']}" />
          <c:set var="description" value="${attributes['description']}" />
          <c:set var="contact" value="${attributes['contact']}" />
          <c:set var="institution" value="${attributes['institution']}" />        
          <c:set var="tables" value="${record.tables}" />
          <c:set var="publications" value="${tables['Publications']}" />
          <c:set var="contacts" value="${tables['Contacts']}" />
          <c:set var="isolates" value="${tables['Isolates']}" />
          <c:set var="externallinks" value="${tables['HyperLinks']}" />
          <c:set var="versions" value="${tables['Version']}" /> 
          <c:set var="references" value="${tables['References']}" />
          <c:set var="genHistory" value="${tables['GenomeHistory']}" />

          <imp:toggle name="${datasetId.value}" displayName="${displayName.value}" isOpen="false">
            <jsp:attribute name="content">

            <%-------    DATASET CONTENT ----------------%>
              <c:if test='${not empty organism.value}'>
                <h3>${organism.displayName}</h3>
                <div>${organism.value}</div>
              </c:if>

              <h3>${contact.displayName}</h3>
              <div>
                <c:if test='${not empty contact.value}'>${contact.value}</c:if>
                <c:if test='${not empty institution.value}'> - ${institution.value}</c:if>
              </div>

              <c:if test='${not empty description.value}'>
                <h3>${description.displayName}</h3>
                <div>${description.value}</div>
              </c:if>

              <%-- avoiding table.tag to unify style with searches --%>
              <c:if test="${fn:length(isolates) > 0}">
                <h3>${isolates.displayName}</h3>
                <div>
                  <ul>
                    <c:forEach items="${isolates}" var="isolate">
                      <li><a href="${isolate['isolate_link'].url}">${isolate['isolate_link'].displayText}</a> </li>
                    </c:forEach>
                  </ul>
                </div>
              </c:if>

              <%-- avoiding table.tag to unify style with searches --%>
              <c:if test="${fn:length(publications) > 0}">
                <h3>${publications.displayName}</h3>
                <div>
                  <ul>
                    <c:forEach items="${publications}" var="publication">
                      <li><a href="${publication['pubmed_link'].url}">${publication['pubmed_link'].displayText}</a></li>
                    </c:forEach>
                  </ul>
                </div>
              </c:if>

              <%-- avoiding table.tag to unify style with searches --%>
              <c:if test="${fn:length(contacts) > 0}">
                <h3>${contacts.displayName}</h3>
                <div>
                  <ul>
                    <c:forEach items="${contacts}" var="contact">
                      <li><c:if test="${contact['contact_name'] != null}">${contact['contact_name']}</c:if> <c:if test="${contact['affiliation'] != null}">(${contact['affiliation']})</c:if></li>
                    </c:forEach>
                  </ul>
                </div>
              </c:if>

              <c:if test="${fn:length(externallinks) > 0}">
                <h3>${externallinks.displayName}</h3>
                <div>
                   <ul>
                    <c:forEach items="${externallinks}" var="externallink">
                      <li><a title="${externallink['description'].value}" href="${externallink['hyper_link'].url}">${externallink['hyper_link'].displayText}</a></li>
                    </c:forEach>
                  </ul>
                </div>
              </c:if>

              <c:if test="${fn:length(genHistory) > 0}">
                <h3>${genHistory.displayName}</h3>
                <div>
                   <table>
                    <c:forEach items="${genHistory}" var="genHistoryRow">
                      <tr><td>${genHistoryRow['build'].displayName}--${genHistoryRow['build']}----${genHistoryRow['release_date']}<br>
                              ${genHistoryRow['note']}<br>
                              Genome source: ${genHistoryRow['genome_source']}--${genHistoryRow['genome_version']}<br>
                              Annotation source: ${genHistoryRow['annotation_source']}--${genHistoryRow['annotation_version']}
                      </td></tr>
                    </c:forEach>
                   </table>
                 </div>
              </c:if>

              <c:if test="${fn:length(versions) > 0}">
                <h3>${versions.displayName}</h3>
                <div>
                  <table>
                    <c:forEach items="${versions}" var="version">
                      <tr><td>${version['version']}(${version['organism']})</td></tr>
                    </c:forEach>
                  </table>
                </div>
              </c:if>

              <c:if test="${fn:length(references) > 0}">
                <c:set var="hasQuestion" value="${false}" />
                <c:set var="referenceContent">
                  <ul>
                    <c:forEach items="${references}" var="reference">
                      <c:if test="${reference['target_type'] eq 'question'}">
                        <jsp:setProperty name="wdkModel" property="questionName" value="${reference['target_name']}" />

                        <c:set var="question" value="${wdkModel.question}" />
                        <c:if test="${question != null}">
                          <c:set var="hasQuestion" value="${true}" />
                          <c:set var="display" value="Identify ${question.recordClass.displayNamePlural} based on ${question.displayName}" />
                          <c:url var="questionUrl" value="/showQuestion.do?questionFullName=${question.fullName}" />
                          <c:choose>
                            <c:when test="${question.isTransform}">
                              <li>${display}</li>
                            </c:when>
                            <c:otherwise>
                              <li><a title="${question.summary}" href="${questionUrl}">${display}</a></li>
                            </c:otherwise>
                          </c:choose>
                        </c:if> 
                      </c:if>
                    </c:forEach>
                  </ul>
                </c:set>
                <c:if test="${hasQuestion}">
                <h3>${references.displayName}</h3>
                <div>${referenceContent}</div>
                </c:if>
              </c:if>

              <imp:profileGraphs type="dataset" tableName="ExampleGraphs"/>

            </jsp:attribute>
          </imp:toggle>
        </c:forEach>       <!-- all datasets in one category  -->

      </jsp:attribute>
    </imp:toggle>
  </c:forEach> <!-- all categories  -->
  

   <%-- if we came to this page to show only a few datasets (would be specified in the url) --%>
  <c:if test="${fn:length(param.reference) > 0}">
    <p style="text-align:center;font-size:120%"><a href="<c:url value='/getDataset.do?display=detail' />">Click here to see the complete list of Data Sets</a></p>
  </c:if>

</div>      <!-- #data-sets   -->

<script>
  !function($) {
    $('.toggle-section')
      .each(function(i, e) {
        var isActive = (e.getAttribute('wdk-active') || '').toLowerCase();
        $(e).accordion({
          collapsible: true,
          active: isActive === 'true' ? 0 : false,
          heightStyle: 'content',
          animate: false,
          create: function(event, ui) {
            var activateOnce = _.once(new Function(e.getAttribute('wdk-onactivate')));
            // panel is not collapsed
            if (ui.panel.length && ui.header.length) {
              $(activateOnce);
            } else {
              $(e).on('accordionactivate', activateOnce);
            }
          },
          activate: function(event, ui) {
            var cookieName = "show" + e.getAttribute('wdk-id');
            var cookieValue = ui.newHeader.length && ui.newPanel.length ? 1 : 0;
            wdk.api.storeIntelligentCookie(cookieName, cookieValue,365);
          }
        })
      })

    $('.record-toolbar a[href="#show-all"]')
      .click(function(e) {
        e.preventDefault();
        $('.toggle-section').accordion('option', 'active', 0);
      })

    $('.record-toolbar a[href="#hide-all"]')
      .click(function(e) {
        e.preventDefault();
        $('.toggle-section').accordion('option', 'active', false);
      })

  }(jQuery);
</script>

</imp:pageFrame>
