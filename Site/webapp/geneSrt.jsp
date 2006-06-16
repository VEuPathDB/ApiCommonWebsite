<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<c:set var="$siteName" value="ApiDB"/>
<c:if test="${param.siteName != null}"><c:set var="siteName" value="${param.siteName}"/></c:if>

<site:header title="${siteName}.org :: Retrieve Gene Sequences"
                 banner="Retrieve Gene Sequences"
                 parentDivision="${siteName}"
                 parentUrl="/home.jsp"
                 divisionName="Retrieve Gene Sequences"
                 division="queries_tools"/>

<c:set var="CGI_URL" value="${applicationScope.wdkModel.properties['CGI_URL']}"/>

<!-- begin page table -->

<table border="0" width="100%" cellpadding="3" cellspacing="0" bgcolor="white" class="thinTopBottomBorders"> 

 <tr>
  <td bgcolor="white" valign="top">

  <form action="${CGI_URL}/geneSrt" method="post">
    <table border="0" width="100%" cellpadding="4">
    <tr><td colspan="2" valign="top"><b>Enter a list of Gene IDs (white space or new line delimited):</b></td><tr>
    <tr><td colspan="2">
            <textarea name="ids" rows="4" cols="60">${param.defaultGeneIds}</textarea>
    </td></tr>

    <tr><td colspan="2">
    <b>Choose the type of sequence:</b>
        <input type="radio" name="type" value="protein">protein
        <input type="radio" name="type" value="CDS">CDS
        <input type="radio" name="type" value="processed_transcript" checked>transcript
        <input type="radio" name="type" value="genomic">genomic
    </td></tr>

    <tr><td colspan="2">
    <b>Choose the region of the sequence(s):</b>
    </td></tr>
    <tr><td colspan="2">
    <table cellpadding="4">
        <tr><td>begin at</td>
            <td align="left">
                <input type="radio" name="upstreamAnchor" value="Start" checked> start<br>
                <input type="radio" name="upstreamAnchor" value="End"> stop<br>
            </td>
            <td align="left">&nbsp;&nbsp;&nbsp;&nbsp;+/-&nbsp;&nbsp;
                             <input name="upstreamOffset" value="0" size="6">residues</td></tr>

        <tr><td>end at</td>
            <td align="left">
                <input type="radio" name="downstreamAnchor" value="Start"> start<br>
                <input type="radio" name="downstreamAnchor" value="End" checked> stop<br>
            </td>
            <td align="left">&nbsp;&nbsp;&nbsp;&nbsp;+/-&nbsp;&nbsp;
                             <input name="downstreamOffset" value="0" size="6"> residues</td></tr>
    </table></td></tr>

        <td align="center"><input name="go" value="Get Sequences" type="submit"/></td></tr>

    </table>
  </form>

  <hr>
  <br>

<b>Help</b>
  <br>
  <br>

Types of sequences:
 <ul>
 <li><i>protein</i>: the predicted translation of the gene
 <li><i>CDS</i>: the coding sequence, including UTRs (introns spliced out)
 <li><i>transcript</i>: the processed transcript, excluding UTRs (introns spliced out)
 <li><i>genomic</i>: a region of the genome.  Genomic sequence is always returned from 5' to 3', on the proper strand
 </ul>

Regions:
  <ul>
  <li><i>relative to sequence start</i>:  to retrieve, eg, the 100 bp upstream genomic region, use "begin at <i>start</i> +/- -100  end at <i>start</i> +/- -1".
  <li><i>relative to sequence stop</i>:  to retrieve, eg, the last 10 amino acids of a protein, use "begin at <i>stop</i> +/- -9  end at <i>stop</i> +/- 0".
  <li><i>relative to sequence start and stop</i>:  to retrieve, eg, a CDS with the  first and last 10 basepairs excised, use: "begin at <i>start</i> +/- 10 end at <i>stop</i> +/- -10".
   </ul>
  </td>
  <td valign="top" class="dottedLeftBorder"></td> 
</tr>
</table> 
 
<site:footer/>
