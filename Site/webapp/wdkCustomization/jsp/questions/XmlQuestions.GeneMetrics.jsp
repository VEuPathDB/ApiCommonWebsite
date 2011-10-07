<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="pg" uri="http://jsptags.com/tags/navigation/pager" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>

<%-- get wdkXmlAnswer saved in request scope --%>
<c:set var="xmlAnswer" value="${requestScope.wdkXmlAnswer}"/>

<c:set var="banner" value="${xmlAnswer.question.displayName}"/>

<c:set var="wdkModel" value="${applicationScope.wdkModel}"/>
<c:set value="${wdkModel.displayName}" var="project"/>

<site:header  title="${project} :: Gene Metrics"
                 banner="${project} Gene Metrics"
                 parentDivision="${project}"
                 parentUrl="/home.jsp"
                 divisionName="allSites"
                 division="geneMetrics"/>

<c:set var="orgWidth" value=""/>  <%-- 4% --%>
<c:set var="ncbiTaxPage" value="http://130.14.29.110/Taxonomy/Browser/wwwtax.cgi?mode=Info&id=237895&lvl=3&p=mapview&p=has_linkout&p=blast_url&p=genome_blast&lin=f&keep=1&srchmode=5&unlock"/>


<table width="100%">
<tr><td><h2>FungiDB Gene Metrics</h2></td>
    <td align="right"><a href="<c:url value="/showXmlDataContent.do?name=XmlQuestions.GenomeDataType"/>">FungiDB Genomes and Data Types >>></a></td>
</tr>
<tr><td colspan="2">
FungiDB is an integrated genomic and functional genomic database for the kingdom Fungi. In its first iteration (released in early 2011), FungiDB contains the genomes of <b>18 Fungi covering 17 species</b> (see below). FungiDB integrates whole genome sequence and annotation and will expand to include experimental data and environmental isolate sequences provided by the community of researchers. The database includes comparative genomics, analysis of gene expression, and supplemental bioinformatics analyses and a web interface for data-mining.

<br><br>
<i style="color:#b45f04">(Please mouse over gene metrics for a definition; mouse over acronyms for the organism full name.)</i><br>
</td></tr>
</table>

<table  class="mytableStyle" width="100%">
<c:choose>
  <c:when test='${xmlAnswer.resultSize == 0}'>
    <tr><td>Not available.</td></tr></table>
  </c:when>
  <c:otherwise>

<%-- Organisms/species grouped by websites, alphabetically in each group --%>
<c:set var="bgcolor" value="#efefef"/> 
<tr class="mythStyle">
    <td style="background-color:white;border-right:3px solid grey;border-top:0px none;border-left:0 none;"></td>
    <td style="border-right:3px solid grey" colspan="8" class="mythStyle">Eurotiomycetes; Ascomycota</td>
    <td style="border-right:3px solid grey" colspan="5" class="mythStyle">Sordariomycetes; Ascomyocta</td>
    <td style="border-right:3px solid grey" colspan="2" class="mythStyle">Saccharomycotina; Ascomyocta</td>
    <td style="border-right:3px solid grey" colspan="2" class="mythStyle">Basidiomycota</td>
    <td class="mythStyle">Mucormycotina; Zygomycota</td>
</tr>
<tr class="mythStyle" style="cursor:pointer">
    <td  style="border-right:3px solid grey" class="mythStyle" title="">Gene Metric</td>
    <td class="mythStyle" title="Aspergillus clavatus NRRL 1"><i>Ac</i></td>
    <td class="mythStyle" title="Aspergillus flavus"><i>Afl</i></td>
    <td class="mythStyle" title="Aspergillus fumigatus Af293"><i>Afu</i></td>
    <td class="mythStyle" title="Aspergillus nidulans FGSC A4"><i>Anid</i></td>
    <td class="mythStyle" title="Aspergillus niger ATCC 1015"><i>Anig</i></td>
    <td class="mythStyle" title="Aspergillus terreus NIH 2624"><i>At</i></td>
    <td class="mythStyle" title="Coccidioides immitis H538.4"><i>CiH</i></td>
    <td  style="border-right:3px solid grey" class="mythStyle" title="Coccidioides immitis RS"><i>CiRS</i></td>
    <td class="mythStyle" title="Fusarium graminearum PH-1 (NRRL 31084)"><i>Fg</i></td>
    <td class="mythStyle" title="Fusarium oxysporum lycopersici f. sp. 4287"><i>Fo</i></td>
    <td class="mythStyle" title="Gibberella moniliformis "><i>Gm</i></td>
    <td class="mythStyle" title="Magnaporthe oryzae 70-15"><i>Mo</i></td>
    <td  style="border-right:3px solid grey" class="mythStyle" title="Neurospora crassa OR74A"><i>Nc</i></td>
    <td class="mythStyle" title="Candida albicans SC5314"><i>Ca</i></td>
    <td  style="border-right:3px solid grey" class="mythStyle" title="Saccharomyces cerevisiae S288c"><i>Sc</i></td>
    <td class="mythStyle" title="Cryptococcus neoformans var. grubii H99"><i>Cn</i></td>
    <td  style="border-right:3px solid grey" class="mythStyle" title="Puccinia graminis f.sp.tritici CRL 75-36-700-3"><i>Pg</i></td>
    <td  style="border-right:3px solid grey" class="mythStyle" title="Rhizopus oryzae RA 99-880"><i>Ro</i></td>
</tr>

  <c:forEach items="${xmlAnswer.recordInstances}" var="record">

	 <c:set var="Metric_Type" value="${record.attributesMap['Metric_Type']}"/>
	 <c:set var="Ac" value="${record.attributesMap['Aspergillus_clavatus']}"/>
	 <c:set var="Afl" value="${record.attributesMap['Aspergillus_flavus']}"/>
	 <c:set var="Afu" value="${record.attributesMap['Aspergillus_fumigatus']}"/>
	 <c:set var="Anid" value="${record.attributesMap['Aspergillus_nidulans']}"/>
 	 <c:set var="Anig" value="${record.attributesMap['Aspergillus_niger']}"/>
	 <c:set var="At" value="${record.attributesMap['Aspergillus_terreus']}"/>
	 <c:set var="CiH" value="${record.attributesMap['Coccidioides_immitis_H538_4']}"/>
	 <c:set var="CiRS" value="${record.attributesMap['Coccidioides_immitis_RS']}"/>
	 <c:set var="Fg" value="${record.attributesMap['Fusarium_graminearum']}"/>
	 <c:set var="Fo" value="${record.attributesMap['Fusarium_oxysporum']}"/>
         <c:set var="Gm" value="${record.attributesMap['Gibberella_moniliformis']}"/>
         <c:set var="Mo" value="${record.attributesMap['Magnaporthe_oryzae']}"/>
         <c:set var="Nc" value="${record.attributesMap['Neurospora_crassa']}"/>
         <c:set var="Ca" value="${record.attributesMap['Candida_albicans']}"/>
	 <c:set var="Sc" value="${record.attributesMap['Saccharomyces_cerevisiae']}"/>
	 <c:set var="Cn" value="${record.attributesMap['Cryptococcus_neoformans']}"/>
	 <c:set var="Pg" value="${record.attributesMap['Puccinia_graminis']}"/>
	 <c:set var="Ro" value="${record.attributesMap['Rhizopus_oryzae']}"/>



<tr class="mytdStyle">
    <td style="border-right:3px solid grey;cursor:pointer" class="mytdStyle" align="left" title="${record.attributesMap['Description']}">${Metric_Type}</td>
    <td class="mytdStyle" align="right">${Ac}</td>
    <td class="mytdStyle" align="right">${Afl}</td>
    <td class="mytdStyle" align="right">${Afu}</td>
    <td class="mytdStyle" align="right">${Anid}</td>
    <td class="mytdStyle" align="right">${Anig}</td>
    <td class="mytdStyle" align="right">${At}</td>
    <td class="mytdStyle" align="right">${CiH}</td>
    <td style="border-right:3px solid grey"  class="mytdStyle" align="right">${CiRS}</td>
    <td style="background-color:${bgcolor}" class="mytdStyle" align="right">${Fg}</td>
    <td style="background-color:${bgcolor}" class="mytdStyle" align="right">${Fo}</td>
    <td style="background-color:${bgcolor}" class="mytdStyle" align="right">${Gm}</td>
    <td style="background-color:${bgcolor}" class="mytdStyle" align="right">${Mo}</td>
    <td style="background-color:${bgcolor};border-right:3px solid grey" class="mytdStyle" align="right">${Nc}</td>
    <td class="mytdStyle" align="right">${Ca}</td>
    <td style="border-right:3px solid grey" class="mytdStyle" align="right">${Sc}</td>
    <td style="background-color:${bgcolor}" class="mytdStyle" align="right">${Cn}</td>
    <td style="background-color:${bgcolor};border-right:3px solid grey" class="mytdStyle" align="right">${Pg}</td>
    <td class="mytdStyle" align="right">${Ro}</td>
</tr>
 
  </c:forEach>



  </table>


  </c:otherwise>
</c:choose>





<site:footer/>
