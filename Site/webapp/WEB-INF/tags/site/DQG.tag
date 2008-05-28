<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>



<div id="contentwrapper">

  <div id="contentcolumn">
	<div class="innertube">
<!--	  <h1>Using ${applicationScope.wdkModel.name}</h1>-->

		<p align="center"><span class="large">Tools:</span> <a href="#"><strong>Genome Browser</strong></a>, <a href="#"><strong>PubMed and Entrez</strong></a>, <a href="#"><strong>BLAST</strong></a>, and <a href="#"><strong>CryptoCyc</strong></a></p> <br>
		
	  <div id="half_right">
	    <form id="form2" name="form1" method="post" action="">
          <label>Text Search:
          <input name="Keyword" type="text" class="search-box" id="Keyword" />
          </label>
          <img src="/assets/images/mag_glass.png" alt="SEARCH!" width="23" height="23" class="img_align_middle" />
                                                </form>
	  </div>
	  <div id="half_left">
	    <form id="form1" name="form1" method="post" action="">
	      <label><strong>Search by:: </strong>Gene ID:
	        <input name="Gene_ID" type="text" class="search-box" id="Gene_ID" />
          </label>
          <img src="/assets/images/mag_glass.png" alt="SEARCH!" width="23" height="23" class="img_align_middle" />
	    </form>
      </div>
      
      <p>&nbsp;</p><p>&nbsp;</p>
      
      <p>&nbsp;</p>
      <p>&nbsp;</p>
      <p>&nbsp;</p>
      <table width="100%" border="0" class="3column">
  <tr>
    <td width="33%" align="center">
	   <c:set var="qSetName" value="GeneQuestions" />
       <site:DQG_bubble 
				banner="bubble_id_genes_by.png" 
				alt_banner="Identify Genes By:" 
				questionSets="GeneQuestions"
	   />
    </td>
    <td width="34%"  align="center">
       <site:DQG_bubble 
				banner="bubble_id_other_data.png" 
				alt_banner="Identify Other Data Types:" 
				questionSets="IsolateQuestions,GenomicSequenceQuestions,SnpQuestions,EstQuestions,OrfQuestions"
		/>
    </td>
    <td width="33%"  align="center">
       <site:DQG_bubble 
				banner="bubble_id_third_option.png" 
				alt_banner="Gene Data:"
       />
	</td>
  </tr>
</table>

	

	</div>
  	</div>
</div>