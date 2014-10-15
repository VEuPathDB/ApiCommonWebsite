<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="w" uri="http://www.servletsuite.com/servlets/wraptag" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<c:set value="${requestScope.wdkRecord}" var="wdkRecord"/>
<c:set var="primaryKey" value="${wdkRecord.primaryKey}"/>
<c:set var="pkValues" value="${primaryKey.values}" />
<c:set var="projectId" value="${pkValues['project_id']}" />
<c:set var="id" value="${pkValues['source_id']}" />

<c:set var="attrs" value="${wdkRecord.attributes}"/>
<c:set var="recordName" value="${wdkRecord.recordClass.displayName}" />

<c:choose>
<c:when test="${!wdkRecord.validRecord}">

<!-----------   INVALID RECORD ----------------------------------->

<imp:pageFrame title="${wdkModel.displayName} : gene ${id}"
             divisionName="Gene Record"
		         refer="recordPage" 
             division="queries_tools">
  <h2 style="text-align:center;color:#CC0000;">The ${fn:toLowerCase(recordName)} '${id}' was not found.</h2>
</imp:pageFrame>
</c:when>

<c:otherwise>    <!-----------  VALID RECORD  ----------------------------------->

<!--  TABLES REFERRED TO IN THIS PAGE, SHOULD EXIST IN ALL MODELS 
- CategoryLink
- UserComments
- GeneModel in Sequences section
- GenomeSequencingAndAnnotationAttribution in Sequences section
-->

<!--  SETTING ATTRIBUTES ****** should al exist in all projects --------->

<c:set var="organismFull" value="${attrs['organism_full'].value}"/>
<c:set var="binomial" value="${attrs['genus_species'].value}"/>
<%-- binomial used in many sections --%>
<%-- organismFull used in expression section  --%>
<!-- example values:
binomial:  	    Plasmodium falciparum
organismFull:   Plasmodium falciparum 3D7 
-->

<c:set var="so_term_name" value="${attrs['so_term_name'].value}"/>
<c:set var="extdbname" value="${attrs['external_db_name'].value}" />
<c:set var="prd" value="${attrs['product'].value}"/>
<c:set var="overview" value="${attrs['overview']}"/>
<c:set var="length" value="${attrs['transcript_length']}"/>
<c:set var="isCodingGene" value="${so_term_name eq 'protein_coding'}"/>
<c:set var="hasPhenotype" value="${attrs['hasPhenotype'].value eq '1'}"/>
<c:set var="async" value="${param.sync != '1'}"/>
<!--  <c:set var="start" value="${attrs['start_min_text'].value}"/>  -->
<c:set var="end" value="${attrs['end_max_text'].value}"/>
<c:set var="sequence_id" value="${attrs['sequence_id'].value}"/>
<c:set var="start" value="${attrs['start_min'].value}"/>
<c:set var="end" value="${attrs['end_max'].value}"/>
<c:set var="context_start_range" value="${attrs['context_start'].value}" />
<c:set var="context_end_range" value="${attrs['context_end'].value}" />
<c:set var="orthomcl_name" value="${attrs['orthomcl_name'].value}"/>

<c:set var="strand" value="+"/>
<c:if test="${attrs['strand'].value == 'reverse'}">
  <c:set var="strand" value="-"/>
</c:if>

<!-- COMMENTS attributes  -->
<c:set value="${wdkRecord.tables['CategoryLink']}" var="ctgLinks"/>

<c:forEach var="row" items="${ctgLinks}">
  <c:set var="ctgLink" value="${row['category_link'].value}"/>
  <c:set var="category" value="${fn:substringBefore(ctgLink, ':')}"/>
  <c:set var="num" value="${fn:substringAfter(ctgLink, ':')}"/>
  <!-- c:set var="cmtLink" value="<a href='#user-comment'>[Link to User Comment]</a>"/ -->
  <c:set var="cmtLink" value=""/>

  <c:choose>
    <c:when test="${category eq 'name/product' || category eq 'function'}">
      <c:set var="has_namefun_comment" value="${cmtLink}"/>
    </c:when>
    <c:when test="${category eq 'phenotype'}">
      <c:set var="has_phenotype_comment" value="${cmtLink}"/>
    </c:when>
    <c:when test="${category eq 'sequence'}">
      <c:set var="has_sequence_comment" value="${cmtLink}"/>
    </c:when>
    <c:when test="${category eq 'gene model'}">
      <c:set var="has_model_comment" value="${cmtLink}"/>
    </c:when>
    <c:when test="${category eq 'expression'}">
      <c:set var="has_expression_comment" value="${cmtLink}"/>
    </c:when>
  </c:choose>
</c:forEach> 




<!-- =========================  HEADER ======================== -->

<imp:pageFrame title="${wdkModel.displayName} : gene ${id} (${prd})"
             divisionName="Gene Record"
		         refer="recordPage" 
             division="queries_tools"
             summary="${overview.value} (${length.value} bp)">

  <script>
    (function($) {
      // prevent FOUC
      $('.innertube').css('opacity', 0);
      $('html').css('overflow', 'hidden');
      $(function() {
        $('.innertube') .css('opacity', 1);
        $('html').css('overflow', '');
      });
    }(jQuery));
  </script>
  <style>
    h1 {
      text-align: left;
      float: left;
    }
    .overview {
      background-color: #f7f7f7;
      border: 1px solid #e6e6e6;
    }
    .overview p {
      max-width: 1000px;
    }
    .overview p, .overview table {
      font-size: 1.2em;
    }
    .overview table{
      margin: .2em 0;
    }
    .overview th {
      white-space: nowrap;
    }
    .overview td {
    }
    .overview th, .overview td {
      border: none;
      vertical-align: top;
      padding: 4px 8px;
    }
    .innertube {
      padding: 6px;
      /*padding-left: 20em;*/
    }
    .innertube > .toggle-section.ui-accordion {
      margin: 6px 0;
    }
    .innertube > .toggle-section.ui-accordion > .ui-accordion-header {
      font-size: 150%;
      background-color: #dfdfdf;
      padding-left: 1.6em;
    }
    .innertube > .toggle-section.ui-accordion > .ui-accordion-content {
      padding: 0 2px;
    }
  </style>

<a name="top"></a>


<!-- =========================  TOP MENU 4 OPTIONS, some genes ONLY 2  ========================= -->
<%--
<table width="100%">
<tr>
  <td align="center" style="padding:6px"><a href="#Annotation">Annotation</a>
    <imp:image src="images/arrow.gif"/>
  </td>

  <c:if test="${isCodingGene}">
  <td align="center"><a href="#Protein">Protein</a>
    <imp:image src="images/arrow.gif"/>
  </td>
  </c:if>

<c:if test="${hasPhenotype}">
  <td align="center"><a href="#Phenotype">Phenotype</a>
    <imp:image src="images/arrow.gif"/>
  </td>
  </c:if>

  <c:if test="${attrs['hasExpression'].value eq '1'}">
  <td align="center"><a href="#Expression">Expression</a>
    <imp:image src="images/arrow.gif"/>
  </td>
  </c:if>

  <td align="center"><a href="#Sequence">Sequence</a>
    <imp:image src="images/arrow.gif"/>
  </td>
</tr>
</table>

<hr>
--%>
<!-- =========================  PAGE BEGINNING: title and stuff under title  ========================= -->

<!-- this block is to set a link to add a comment  -->
<c:set var="externalDbName" value="${attrs['external_db_name']}"/>
<c:set var="externalDbVersion" value="${attrs['external_db_version']}"/>
<c:url var="commentsUrl" value="addComment.do">
  <c:param name="stableId" value="${id}"/>
  <c:param name="commentTargetId" value="gene"/>
  <c:param name="externalDbName" value="${externalDbName.value}" />
  <c:param name="externalDbVersion" value="${externalDbVersion.value}" />
  <c:param name="organism" value="${binomial}" />
  <c:param name="locations" value="${fn:replace(start,',','')}-${fn:replace(end,',','')}" />
  <c:param name="contig" value="${attrs['sequence_id'].value}" /> 
  <c:param name="strand" value="${strand}" />
  <c:param name="flag" value="0" /> 
  <c:param name="bulk" value="0" /> 
</c:url>


 <c:choose>
  <c:when test="${attrs['is_unassigned_tcruzi'].value  == 1}">	
      <c:set var="append" value=" - (this contig could not be assigned to Esmeraldo or Non-Esmeraldo)" />
    </c:when>
    <c:otherwise>
      <c:set var="append" value="" />
    </c:otherwise>
 </c:choose>

<!------------ BIG ID title  ------------->
<div class="ui-helper-clearfix">
  <h1>${recordName} ${id}</h1>

  <div class="record-toolbar ui-widget" style="float: left; margin: 0 0 0 1em;">
    <a href="${commentsUrl}">
      Add a Comment<span class="ui-icon ui-icon-comment"></span>
      <!-- <img src="/assets/images/commentIcon12.png"/> -->
    </a>
    <imp:recordPageBasketIcon />
    <a href="#download">
      Download ${recordName}<span class="ui-icon ui-icon-disk"></span>
    </a>
  </div>
</div>

<div class="overview">
  <table>
    <tr><th>Product</th><td>${prd}</td></tr>
    <c:if test="${attrs['is_genedb_organism'].value == 1 and attrs['new_product_name'].value != null}">
      <tr><th>Updated product from GeneDB</th><td>${attrs['new_product_name']}</td></tr>
    </c:if>
    <tr><th>Organism</th><td>${attrs['organism']}</td></tr>
    <tr><th>Type</th><td>${attrs['gene_type']}</td></tr>
    <tr><th>Location</th><td>${attrs['location_text']}</td></tr>
    <c:if test="${attrs['old_ids'].value != null && attrs['old_ids'].value ne id }">
      <tr><th>Previous IDs</th><td>${attrs['old_ids']}</td></tr>
    </c:if>
    <c:set var="commentsLength" value="${fn:length(wdkRecord.tables['UserComments'])}"/>
    <c:if test="${commentsLength gt 0}">
      <tr><th>User comments</th><td><a href="${commentsUrl}">Read ${commentsLength} user comments</a></td></tr>
    </c:if>
    <c:if test="${attrs['is_genedb_organism'].value == 1 and attrs['updated_annotation'].value != null}">
      <tr><th>Updated annotation</th><td><a href="${attrs['updated_annotation']}">View at GeneDB</a></td>
    </c:if>
    <c:if test="${projectId ne 'TrichDB' && attrs['is_annotated'].value == 0}">
      <tr>
        <th>Genome status</th>
        <c:choose>
          <c:when test="${attrs['release_policy'].value  != null}">
            <td>${attrs['release_policy'].value }</td>
          </c:when>
          <c:otherwise>
            <td>The data for this genome is unpublished. You should consult with the Principal Investigators before undertaking large scale analyses of the annotation or underlying sequence.</td>
          </c:otherwise>
        </c:choose>
      </tr>
    </c:if>
  </table>


  <!--------------  NOTE on Unpublished data as it was in Plasmo page ----------------------->

</div>


<div class="record-toolbar ui-widget ui-helper-clearfix">
  <a href="#show-all">
    Expand all<span class="ui-icon ui-icon-arrowthickstop-1-s"></span>
  </a>
  <a href="#hide-all">
    Collapse all<span class="ui-icon ui-icon-arrowthickstop-1-n"></span>
  </a>
</div>

<!-- TODO Figure out if this should be kept - residue from sync merge -->
<!--------------  NOTE on data with ReleasePolicy, or default text for Unpublished data ---------------->
<c:if test="${projectId ne 'TrichDB' }">
  <c:choose>
  <c:when test="${attrs['release_policy'].value  != null}">
    <b>NOTE: ${attrs['release_policy'].value }</b>
  </c:when>
  <c:otherwise>
    <c:if test="${attrs['is_annotated'].value == 0}">
    <b>NOTE: The data for this genome is unpublished. You should consult with the Principal Investigators before undertaking large scale analyses of the annotation or underlying sequence.</b>
    </c:if>
  </c:otherwise>
  </c:choose>
</c:if>
<!-- ENDTODO -->

<imp:toggle name="General" displayName="General" isOpen="true">
  <jsp:attribute name="content">

<%--##########################  SECTION  BEFORE ANNOTATION   ################################--%>

<%----giardia COMMUNITY EXPERT ANNOTATION -----------%>
<imp:wdkTable2 tblName="CommunityExpComments" isOpen="true" attribution="" />

<%-- OVERVIEW ------------%>
<%--
<c:set var="attr" value="${attrs['overview']}" />

<c:if test="${attrs['is_deprecated'].value eq 'Yes'}">
   <c:set var="isdeprecated">
     **<b>Deprecated</b>**
   </c:set>
</c:if>

<imp:toggle name="${attr.name}"
    displayName="${attr.displayName} ${has_namefun_comment}"
    isOpen="true"
    content="${attr.value}${append}" />
--%>

<c:set var="dna_gtracks" value="${attrs['dna_gtracks'].value}"/>
<c:set var="protein_gtracks" value="${attrs['protein_gtracks'].value}"/>


<%-- DNA CONTEXT ------------%>
<c:if test="${dna_gtracks ne ''}">

  <c:set var="lowerProjectId" value="${fn:toLowerCase(projectId)}"/>
  <c:set var="gnCtxUrl"> /cgi-bin/gbrowse_img/${lowerProjectId}/?name=${sequence_id}:${context_start_range}..${context_end_range};hmap=gbrowseSyn;l=${dna_gtracks};width=800;embed=1;h_feat=${fn:toLowerCase(id)}@yellow;genepage=1
  </c:set>

  <c:set var="gnCtxDivId" value="gnCtx"/>

  <c:set var="gnCtxImg">
		<c:set var="gbrowseUrl">
        /cgi-bin/gbrowse/${lowerProjectId}/?name=${sequence_id}:${context_start_range}..${context_end_range};h_feat=${fn:toLowerCase(id)}@yellow
    </c:set>

    <center>
		    <a id="gbView" href="${gbrowseUrl}">View in Genome Browser</a>
				<div>(<i>use right click or ctrl-click to open in a new window</i>)</div>
				<div id="${gnCtxDivId}"></div>
		    <a id="gbView" href="${gbrowseUrl}">View in Genome Browser</a>
				<div>(<i>use right click or ctrl-click to open in a new window</i>)</div>
		</center>

  </c:set>

  <imp:toggle 
    name="dnaContextSyn" displayName="Genomic Context"
    displayLink="${has_model_comment}"
    content="${gnCtxImg}" isOpen="true" 
    imageMapDivId="${gnCtxDivId}" imageMapSource="${gnCtxUrl}"
    postLoadJS="/gbrowse/apiGBrowsePopups.js,/gbrowse/wz_tooltip.js"
    attribution=""
    dsLink="/cgi-bin/gbrowse_citation.pl?project_id=${projectId}&tracks=${dna_gtracks}"
  />

</c:if> 

<%-- END DNA CONTEXT --------------------------------------------%>

<%-- mouseOver does not function properly
     if dnaContext and proteinFeatures imageMap are dynamically set on the page 
 <c:set var="okDOMInnerHtml" value="${ fn:contains(header['User-Agent'], 'Firefox') ||
                                       fn:contains(header['User-Agent'], 'Netscape') }"/>
--%>

<%-- === from toxo ===== --%>
<!-- strains comparison table -->
<imp:wdkTable2 tblName="Strains" isOpen="true"  attribution=""/>


<%---------- HTS SNP OVERVIEW --------- BASED ON ATTRIBUTE  ------%>
<c:if test="${attrs['hasHtsSnps'].value eq '1'}">
<c:set var="htsSNPs" value="${attrs['snpoverview']}" />
<imp:toggle name="${htsSNPs.name}"
    displayName="${htsSNPs.displayName}"
    isOpen="true"
    content="${htsSNPs.value}${append}" />
<imp:wdkTable2 tblName="SNPsAlignment" isOpen="false" /> 
</c:if>


<%-- === from toxo ==  giardia also uses it ==== --%>
<!-- locations -->
<imp:wdkTable2 tblName="GeneLocation" isOpen="true" attribution="" />



<%------------ eQTL regions ---------------%>
<c:set var="queryURL">
  showQuestion.do?questionFullName=GeneQuestions.GenesByEQTL_HaploGrpSimilarity&value%28lod_score%29=1.5&value%28percentage_sim_haploblck%29=25&value%28pf_gene_id%29=${id}&weight=10
</c:set>
<c:set var="extraInfo">
  <a id="assocQueryLink" href="${queryURL}">Other genes that have similar associations based on eQTL experiments</a><br>(<i>use right click or ctrl-click to open in a new window</i>)
</c:set>
<imp:wdkTable2 tblName="Plasmo_eQTL_Table" isOpen="true" attribution="" postscript="${extraInfo}" />


<%------------- version 8.2 genes - WHICH SITEs?-----------%>
<imp:wdkTable2 tblName="PreviousReleaseGenes" isOpen="true" attribution="" />


<%----------- Mercator / Mavid alignments ------------%>
<%-- asked JB how to handle this: remove conditionals
- In crypto: it was done always
- In toxo: it was done for all BUT not for externalDbName.value:  'Roos Lab T. gondii apicoplast
          <c:if test="${externalDbName.value ne 'Roos Lab T. gondii apicoplast'}">
- in plasmo: it was done ONLY if externalDbName.value eq 'Pfalciparum_chromosomes_RSRC
--%>
<c:if test="${strand eq '-'}">
   <c:set var="revCompOn" value="1"/>
</c:if>


<imp:mercatorTable tblName="MercatorTable" isOpen="false" 
     cgiUrl="/cgi-bin" projectId="${projectId}" 
     revCompOn="${revCompOn}" contigId="${sequence_id}" 
     start="${start}" end="${end}" /> 


<%-------COMMENT OUT FOR DEBUGGING: MetaTable --%> 
<%--
<imp:wdkTable2 tblName="MetaTable" isOpen="FALSE" attribution=""/>
--%>

  </jsp:attribute>
</imp:toggle>


<%--##########################   ANNOTATION      ################################--%>
<imp:toggle name="Annotation" displayName="Annotation" isOpen="true">
  <jsp:attribute name="content">

<imp:wdkTable2 tblName="UserComments" isOpen="true" attribution="">
  <jsp:attribute name="preamble">
    <!-- User comments -->
    <a name="user-comment"/>
    <b><a title="Click to go to the comments page" style="font-size:120%" href="${commentsUrl}">Add a comment on ${id}
      <imp:image style="position:relative;top:2px" width="28" src="images/commentIcon12.png"/>
    </a></b>
    <br><br>
  </jsp:attribute>
</imp:wdkTable2>


<!-- EC number -->
<a name="ecNumber"></a>
<c:if test="${isCodingGene}">
  <imp:wdkTable2 tblName="EcNumber" isOpen="false" attribution=""/>
</c:if>


<!-- metabolic pathways -->
<imp:wdkTable2 tblName="CompoundsMetabolicPathways" isOpen="true" attribution=""/>


<!-- Giardia: Gene Deprecation:  TODO.  Temporarily remove because not loaded in rebuild --> 
<%-- imp:wdkTable tblName="GeneDeprecation" isOpen="true"/ --%>


<!-- External Links --> 
<imp:wdkTable2 tblName="GeneLinkouts" isOpen="true" attribution=""/>

<!-- Orthologs and Paralogs -->
<c:if test="${isCodingGene}">
  <c:set var="orthomclLink">
  <c:choose>
    <c:when test="${fn:contains( orthomcl_name, '|') }">
    <div>
    <br>Note: Genes in this table could not be mapped to OrthoMCL, but were grouped to each other based on blast similarity.
    </div>
    </c:when>
    <c:otherwise>
    <div>
    <br> <a title="This gene maps to an existing 'group' in OrthoMCL.org. Click on this link to get information about the 'group', for example descriptions of the other gene members. 

It is possible that this gene will not show as a member in this 'group' at the OrthoMCL.org website; OrthoMCL.org contains data (version 5) from a few years ago, which might not have included this gene.

We are currently in the process of creating an updated version 6 of OrthoMCL.org." target="_blank" href="<imp:orthomcl orthomcl_name='${orthomcl_name}'/>">View the group (${orthomcl_name}) containing this gene (${id}) in the OrthoMCL database</a>
    </div>
    </c:otherwise>
  </c:choose>
  </c:set>

  <imp:wdkTable2 tblName="Orthologs" isOpen="false" attribution=""
                 postscript="${orthomclLink}"/>
</c:if>


<!-- gene alias table -->
<imp:wdkTable2 tblName="Alias" isOpen="FALSE" attribution=""/>


<!-- External Links --> 
<imp:wdkTable2 tblName="GeneLinkouts" isOpen="true" attribution=""/>


<!-- Hagai -->
<c:if test="${isCodingGene}">
  <imp:wdkTable2 tblName="MetabolicPathways" attribution=""/>
</c:if>

<!-- metabolic pathways -->
<imp:wdkTable2 tblName="CompoundsMetabolicPathways" isOpen="true" attribution=""/>

<!-- EC number -->
<a name="ecNumber"></a>
<c:if test="${isCodingGene}">
  <imp:wdkTable2 tblName="EcNumber" isOpen="false" attribution=""/>
</c:if>

<!-- GO TERMS -->
<c:if test="${isCodingGene}">
  <a name="goTerm"></a>
  <c:set var="goEvidenceLink">
    <div>
    <br> <a target="_blank" href="http://www.geneontology.org/page/introduction">View documentation on GO Evidence Codes</a>
    </div>
  </c:set>
  <imp:wdkTable2 tblName="GoTerms" attribution="" postscript="${goEvidenceLink}"/>
</c:if>

<!-- Notes from annotator == in toxo only shown if externalDbName.value eq 'Roos Lab T. gondii apicoplast-->
<imp:wdkTable2 tblName="Notes" attribution="" />


<!-- phenotype -->
<imp:wdkTable2 tblName="RodMalPhenotype" isOpen="false"  attribution=""/>

<%-- mr4reagents  --%>
<imp:wdkTable2 tblName="Mr4Reagents" attribution=""/>


<%-- PlasmoGem --%>
<c:if test="${attrs['has_plasmogem_info'] eq '1'}">
  <imp:toggle 
    name="PlasmoCyc"
    displayName="PlasmoCyc"
    isOpen="true"
    content="Query Pathway/Genome Databases at <a target='_blank' href='${plasmocycurl}'>PlasmoCyc</a>"/>
</c:if>

<%-- from giardia new in build21--%>
<c:if test="${projectId eq 'GiardiaDB' && attrs['has_image'].value eq '1'}">
  <imp:wdkTable tblName="CellularLocalization" isOpen="true" attribution=""/>
</c:if> 


<!-- Giardia: Gene Deprecation:  TODO.  Temporarily remove because not loaded in rebuild --> 
<%-- imp:wdkTable tblName="GeneDeprecation" isOpen="true"/ --%>

<%-- was already commented out
<imp:wdkTable2 tblName="AnnotationChanges"/>
--%>

  </jsp:attribute>
</imp:toggle>


<%--##########################   PROTEIN      ################################--%>

<c:if test="${isCodingGene}">
  
<imp:toggle name="Protein" displayName="Protein" isOpen="true">
  <jsp:attribute name="content">


<%-- Protein Features------------%>
<c:set var="proteinLength" value="${attrs['protein_length'].value}"/>
<c:set var="proteinFeaturesUrl">
   http://${pageContext.request.serverName}/cgi-bin/gbrowse_img/${lowerProjectId}aa/?name=${id}:1..${proteinLength};l=${protein_gtracks};hmap=pbrowse;width=800;embed=1;genepage=1
</c:set>

<c:if test="${protein_gtracks ne ''}">
  <c:set var="proteinFeaturesImg">
  <%--
    <noindex follow><center>
    <c:catch var="e">
      <c:import url="${proteinFeaturesUrl}"/>
    </c:catch>
    <c:if test="${e!=null}">
      <imp:embeddedError 
            msg="<font size='-2'>temporarily unavailable</font>" 
            e="${e}" />
    </c:if> 
    </center></noindex>
    --%>
    <center>
      <wdk-ajax manual url="${proteinFeaturesUrl}"></wdk-ajax>
    </center>
  </c:set>

  <imp:toggle name="proteinContext"  displayName="Protein Features" 
                content="${proteinFeaturesImg}" 
                isOpen="true"
                attribution=""
    dsLink="/cgi-bin/gbrowse_citation.pl?project_id=${projectId}aa&tracks=${protein_gtracks}"
/>
</c:if> 


<%-- Y2Hinteractions ------------%>
<imp:wdkTable2 tblName="Y2hInteractions" isOpen="true" attribution=""/>


<!-- Molecular weight -->
<c:set var="mw" value="${attrs['molecular_weight'].value}"/>
<c:set var="min_mw" value="${attrs['min_molecular_weight'].value}"/>
<c:set var="max_mw" value="${attrs['max_molecular_weight'].value}"/>

<c:choose>
  <c:when test="${min_mw != null && max_mw != null && min_mw != max_mw}">
    <imp:toggle 
      name="Molecular Weight"
      displayName="Molecular Weight"
      isOpen="true"
      content="${min_mw} to ${max_mw} Da" />
  </c:when>
  <c:otherwise>
    <imp:toggle 
      displayName="Molecular Weight"
      name="Molecular Weight"
      isOpen="true"
      content="${mw} Da" />
  </c:otherwise>
</c:choose>

<!-- Isoelectric Point -->
<c:set var="ip" value="${attrs['isoelectric_point']}"/>

        <c:choose>
            <c:when test="${ip.value != null}">
             <imp:toggle
                name="${ip.displayName}"
                displayName="${ip.displayName}"
                isOpen="true"
                 content="${ip.value}" />
            </c:when>
            <c:otherwise>
             <imp:toggle
                name="${ip.displayName}"
                displayName="${ip.displayName}"
                isOpen="true"
                 content="N/A" />
            </c:otherwise>
        </c:choose>


<!-- Mass Spec -->
<c:if test="${attrs['hasProteomics'].value eq '1'}">
 <imp:wdkTable2 tblName="MassSpec" isOpen="true"   attribution=""/>
</c:if>


<!-- Mass Spec Post Trans Mods -->
<c:if test="${attrs['hasPostTransMod'].value ne '0'}">
 <imp:wdkTable2 tblName="MassSpecMod" isOpen="true"   attribution=""/>
</c:if>


<%-- Pberghei Prot Expression  --%>
<imp:wdkTable2 tblName="ProteinExpression" attribution=""/>

<c:if test="${attrs['hasQuantitativeProteomics'].value eq '1'}">
   <imp:profileGraphs species="${binomial}" tableName="ProteinExpressionGraphs"/>
</c:if>

<%--  Protein Linkouts     --%>
<imp:wdkTable2 tblName="ProteinDatabase"/>


<!-- PdbSimilarities -->
<imp:wdkTable2 tblName="PdbSimilarities" postscript="${attrs['pdb_blast_form'].value}" attribution=""/>


<!-- SSGCID  ******* not in Datasets because we do not load the dataset -->
<c:if test="${attrs['hasSsgcid'].value eq '1'}">
  <imp:wdkTable2 tblName="Ssgcid" isOpen="true" attribution="" />
</c:if>

<!-- SSGCID Note  -->
<c:if test="${attrs['hasSsgcid'].value eq '0' && attrs['hasPdbSimilarity'].value eq '0'}">
  ${attrs['ssgcid_request_link']}
</c:if>


<!-- Antibody  -->
<imp:wdkTable2 tblName="Antibody" attribution="" />


<!-- 3D struct predictions ==== only 3D7   -->
<imp:wdkTable2 tblName="3dPreds" attribution=""/>


<!-- Epitopes -->
<imp:wdkTable2 tblName="Epitopes"/>


  </jsp:attribute>
</imp:toggle>

</c:if> <%-- end if isCodingGene --%>


<%--######################   PHENOTYPE    ################################--%>
<c:if test="${hasPhenotype}">

<imp:toggle name="Phenotype" displayName="Phenotype" isOpen="true">
  <jsp:attribute name="content">

<c:set var="geneDbLink">
  <div align="left">
    <br><small>Phenotypes curated from the literature by <a href="http://www.genedb.org/">Gene<b>DB</b></a>
</small></div>
</c:set>

<imp:wdkTable2 tblName="Phenotype" isOpen="true" attribution="" 
               postscript="${geneDbLink}"/>

<imp:profileGraphs species="${binomial}" tableName="PhenotypeGraphs"/>

  </jsp:attribute>
</imp:toggle>

</c:if>
<%--##########################   EXPRESSION      ################################--%>


<c:if test="${attrs['hasExpression'].value eq '1'}">
  <imp:toggle name="Expression" displayName="Expression" isOpen="true">
    <jsp:attribute name="content">

  <imp:expressionGraphs organism="${organismFull}" species="${binomial}"/>
  <imp:wdkTable2 tblName="SpliceSites" isOpen="false" attribution=""/>
  <imp:wdkTable2 tblName="PolyASites" isOpen="false" attribution=""/>
  <imp:wdkTable2 tblName="SageTags" attribution=""/>

    </jsp:attribute>
  </imp:toggle>
</c:if>


<%--##########################  HOST RESPONSE      ################################--%>
<c:if test="${attrs['hasHostResponse'].value eq '1'}">
  <imp:toggle name="HostResponse" displayName="Host Response" isOpen="true">
    <jsp:attribute name="content">

  <imp:profileGraphs species="${binomial}" tableName="HostResponseGraphs"/>

    </jsp:attribute>
    </imp:toggle>
</c:if>

 

<%--##########################   SEQUENCE     ################################--%>

<imp:toggle name="Sequence" displayName="Sequence" isOpen="true">
  <jsp:attribute name="content">
<i>Please note that UTRs are not available for all gene models and may result in the RNA sequence (with introns removed) being identical to the CDS in those cases.</i>

<c:if test="${isCodingGene}">
  <!-- protein sequence -->
  <c:set var="proteinSequence" value="${attrs['protein_sequence']}"/>
  <c:set var="proteinSequenceContent">
    <pre><w:wrap size="60">${attrs['protein_sequence'].value}</w:wrap></pre>
    Sequence Length: ${fn:length(proteinSequence.value)} aa<br/>
  </c:set>
  <imp:toggle name="proteinSequence" displayName="${proteinSequence.displayName}"
             content="${proteinSequenceContent}" isOpen="false"/>
</c:if>

<!-- transcript sequence -->
<c:set var="transcriptSequence" value="${attrs['transcript_sequence']}"/>
<c:set var="transcriptSequenceContent">
  <pre><w:wrap size="60">${transcriptSequence.value}</w:wrap></pre>
  Sequence Length: ${fn:length(transcriptSequence.value)} bp<br/>
</c:set>
<imp:toggle name="transcriptSequence"
             displayName="${transcriptSequence.displayName}"
             content="${transcriptSequenceContent}" isOpen="false"/>


<!-- genomic sequence -->
<c:set value="${wdkRecord.tables['GeneModel']}" var="geneModelTable"/>

<c:set var="i" value="0"/>
<c:forEach var="row" items="${geneModelTable}">
  <c:set var="totSeq" value="${totSeq}${row['sequence'].value}"/>
  <c:set var="i" value="${i +  1}"/>
</c:forEach>

<c:set var="seq">
 <pre><w:wrap size="60" break="<br>">${totSeq}</w:wrap></pre>
  Sequence Length: ${fn:length(totSeq)} bp<br/>
</c:set>

<c:set var="downloadLink">
  <a href="/cgi-bin/contigSrt?project_id=${projectId}&ids=${sequence_id}&start=${start}&end=${end}&go=Get+Sequence">Download</a>
</c:set>
<imp:toggle name="genomicSequence" isOpen="false"
    displayName="Genomic Sequence (introns shown in lower case)"
    content="${seq}" displayLink="${downloadLink}"/>



<c:if test="${isCodingGene}">
  <!-- CDS -->
  <c:set var="cds" value="${attrs['cds']}"/>
  <c:set var="cdsContent">
    <pre><w:wrap size="60">${cds.value}</w:wrap></pre>
    Sequence Length: ${fn:length(cds.value)} bp<br/>
  </c:set>
  <imp:toggle name="cds" displayName="${cds.displayName}"
             content="${cdsContent}" isOpen="false"/>
</c:if>


<!-- attribution -->
<c:set value="${wdkRecord.tables['GenomeSequencingAndAnnotationAttribution']}" var="referenceTable"/>

<c:set value="Error:  No Attribution Available for This Genome!!" var="reference"/>
<c:forEach var="row" items="${referenceTable}">
    <c:set var="reference" value="${row['description'].value}"/>
</c:forEach>

<imp:toggle 
    name="Genome Sequencing and Annotation by:"
    displayName="Genome Sequencing and Annotation by:"
    isOpen="true"
    content="${reference}" />

    </jsp:attribute>
  </imp:toggle>

<%------------------------------------------------------------------%>

<%-- jsp:include page="/include/footer.html" --%>

<script>
  !function($) {
    // register collapsible regions
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
      // .sortable({
      //   axis: 'y',
      //   handle: 'h3',
      //   containment: 'parent',
      //   stop: function(event, ui) {
      //     ui.item.children( "h3" ).triggerHandler( "focusout" );
      //   }
      // });

    $('.record-toolbar a[href="#show-all"]').click(function(e) {
      e.preventDefault();
      $('.toggle-section').accordion('option', 'active', 0);
    });
    $('.record-toolbar a[href="#hide-all"]').click(function(e) {
      e.preventDefault();
      $('.toggle-section').accordion('option', 'active', false);
    });

  }(jQuery);
</script>

<imp:script src="wdkCustomization/js/records/allRecords.js"/>

</imp:pageFrame>
</c:otherwise>
</c:choose>

<imp:pageLogger name="gene page" />
