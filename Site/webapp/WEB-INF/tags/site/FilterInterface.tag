
<script type="text/javascript" src="js/lib/jquery-1.2.3.js"></script>
<script type="text/javascript" src="js/filter_menu.js"></script>
<link rel="StyleSheet" href="misc/filter_menu.css" type="text/css"/>
<div id="crumb_div">
	<p><a href="#" id="filter_link">Create Filter</a></p>
</div><!-- End Crumb Div -->
<div id="filter_div">
<span id="instructions">Choose a query to use as a filter form the list below.  The individual queries will expand when you mouse over the categories.</span>
<table>
<tr>
<td>
<div id="query_selection">
<ul class="top_nav">
	<li><a href="#">Genomic Position</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByLocation">Chromosomal Location</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByCentromereProximity">Proximity to Centromeres</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByTelomereProximity">Proximity to Telomeres</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesbyNonnuclearLocation">Nonnuclear Genomes</a></li>
		</ul>
	</li>
	<li><a href="#">Gene Attributes</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByGeneType">Type (e.g. rRNA, tRNA)</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByExonCount">Exon/Intron Structure</a></li>
		</ul>
	</li>
	<li><a href="#">Other Attributes</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByTextSearch">Keyword</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GeneByLocusTag">List of ID's</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByTaxon">Species</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByMr4Reagents">Available Reagents</a></li>
		</ul>
	</li>
	<li><a href="#">Transcript Expression</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByESTOverlap">EST Evidence</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesBySageTag">SAGE Tag Evidence</a></li>
			<li><a href="showQuestion.do?questionFullName=InternalQuestions.GenesByMicroarrayEvidence">Microarray Evidence</a></li>
		</ul>
	</li>
	<li><a href="#">Protein Expression</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=InternalQuestions.GenesByMassSpecEvidence">Mass Spec. Evidence</a></li>	
		</ul>
	</li>
	<li><a href="#">Similarity/Pattern</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByMotifSearch">Protein Motif</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByInterproDomain">Interpro/Pfam Domain</a></li>
			<li><a href="showQuestion.do?questionFullName=Universalquestions.UnifiedBlast&target=GENE">BLAST Similarity</a></li>
		</ul>
	</li>
	<li><a href="#">Predicated Proteins</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByMolecularWeight">Molecular Weight</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesbyIsoelectricPoint">Isoelectric Point</a></li>
			<li><a href="showQuestion.do?questionFullName=InternalQuestions.GenesByProteinStructure">Protein Structure</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesWithEpitopess">Epitopes</a></li>
		</ul>
	</li>
	<li><a href="#">Putative Function</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByGoTerm">GO Term</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByEcNumber">EC Number</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByMetabolicPathway">Metabolic Pathway</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByProteinProteinInteraction">Y2H Interaction</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByFunctionalInteraction">Predicted Interaction</a></li>
		</ul>
	</li>
	<li><a href="#">Cellular Location</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesWithSignalPeptide">Signal Peptide</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByTransmembraneDomains">Transmembrane Domain</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesBySubcelluarLocalization">Organelle Compartment</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByExportedPrediction">Exported to Host</a></li>
		</ul>
	</li>
	<li><a href="#">Evolution</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesOrthologousToAGivenGene">Orthologs/Paralogs</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByOrthologPattern">Orthology Profile</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByPhyleticProfile">Homology Profile</a></li>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesByPhylogeneticTree">Phylogenetic Tree</a></li>
		</ul>
	</li>
	<li><a href="#">Population Biology</a>
		<ul>
			<li><a href="showQuestion.do?questionFullName=GeneQuestions.GenesBySnps">SNP's</a></li>
		<!--	<li><a href="showQuestion.do?questionFullName=GeneQuestions.">Microsatellites</a></li>-->
		</ul>
	</li>
</ul>
</div><!-- End of Query Selection Div -->
</td>
<td>
<div id="query_form">
</div><!-- End of Query Form Div -->
</td>
</tr>
</table>
</div><!-- End of Filter div -->
<hr id="bottom_filter_line">
