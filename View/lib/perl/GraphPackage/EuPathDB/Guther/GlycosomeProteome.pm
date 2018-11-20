package ApiCommonWebsite::View::GraphPackage::EuPathDB::Guther::GlycosomeProteome;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $geneId = $self->getId();

  my %colors = ( '0' => ['red2'],     # 0: "unlikely to be glycosomal"
	       '1' => ['darkgray'], # 1: "possible glycosomal"
	       '2' => ['green2']    # 2: "high-confidence glycosomal"
	       );

  # query to get category, to decide the color
  my $dbh = $self->getQueryHandle();
  my $sql = "SELECT nfe.categorical_value AS cat_val
FROM results.nafeatureexpression nfe
 , apidbtuning.transcriptattributes ga
 , study.protocolappnode pan
 , study.studylink sl
 , study.study ps
 , study.study i
 , sres.externaldatabaserelease r
 , sres.externaldatabase d
WHERE ga.gene_na_feature_id = nfe.na_feature_id
AND nfe.protocol_app_node_id = pan.protocol_app_node_id
AND pan.protocol_app_node_id = sl.protocol_app_node_id
AND sl.study_id = ps.study_id
AND ps.investigation_id = i.study_id
AND i.external_database_release_id = r.external_database_release_id
AND r.external_database_id = d.external_database_id
AND d.NAME ='tbruTREU927_quantitative_massSpec_Guther_glycosomal_proteome_RSRC'
AND ga.gene_source_id = '$geneId'
";
  my $sh = $dbh->prepare($sql);
  $sh->execute();
  my ($colorNum) = $sh->fetchrow_array();


  my @profileSetsArray = (['Procyclic stage glycosome proteome', 'values', ]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $quant = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::QuantMassSpec->new(@_);
  $quant->setProfileSets($profileSets);
  $quant->setColors( $colors{$colorNum});
  $quant->setForceHorizontalXAxis(0);
  $quant->setYaxisLabel('Log2(H/L)');

  my $sampleLabel = ["Confidence Group"];
  $quant->setSampleLabels($sampleLabel);

  $self->setGraphObjects($quant,);
}

1;
