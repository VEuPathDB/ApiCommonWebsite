package ApiCommonWebsite::View::GraphPackage::Templates::StrandSpecific;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot;
use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;
use EbrcWebsiteCommon::Model::CannedQuery::SenseAntisenseX;
use EbrcWebsiteCommon::Model::CannedQuery::SenseAntisenseY;
use Data::Dumper;
use strict;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  my $id = $self->getId();
  my $cgiApp = $self->getCgiApp();
  my $floor = $cgiApp->param('floor');

  unless ($floor) {$floor=1;}
  my $antisenseFoldChange = $cgiApp->param('antisenseFC');
  unless ($antisenseFoldChange) {$antisenseFoldChange=1;}
  my $senseFoldChange = $cgiApp->param('senseFC');
  unless ($senseFoldChange) {$senseFoldChange=-1;}

  my $graphObjects = $self->getGraphObjects();
  my @profileSets;

  foreach my $graphObject (@{$graphObjects}) {
      if ($graphObject->isa("EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::RNASeqSenseAntisense")) {
	  my @profileSetNames = map{$_->getName()} @{$graphObject->getProfileSets()};
	  my $strandHash = $graphObject->getStrandDictionaryHash();
	  my ($senseId, $antisenseId);
	  if ($profileSetNames[0] =~ /\s(\w+strand)\s/ && $strandHash->{$1} eq "sense") {
	      $senseId = $profileSetNames[0];
	      $antisenseId = $profileSetNames[1];
	  } else {
	      $senseId = $profileSetNames[1];
	      $antisenseId = $profileSetNames[0]; 
	  }
 
         my $valuesQuery = EbrcWebsiteCommon::Model::CannedQuery::SenseAntisenseY->new(Name => "values_SenseAntisense", Id => $id, ProfileSetId => $antisenseId, AntisenseFloor => $floor);

	 my $namesQuery = EbrcWebsiteCommon::Model::CannedQuery::SenseAntisenseX->new(Name => "names_SenseAntisense", Id => $id, ProfileSetId => $senseId, AntisenseFloor => $floor);

	 my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
	 $profileSet->setProfileCannedQuery($valuesQuery);

	 $profileSet->setProfileNamesCannedQuery($namesQuery);

	 push @profileSets, $profileSet;
      }   
  }

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot->new(@_);

  $scatter->setProfileSets(\@profileSets);
  $scatter->setYaxisLabel("log2 (antisense fold-chg)");
  $scatter->setXaxisLabel("log2 (sense fold-chg)");
  $scatter->setColors(["black"]);
  $scatter->setPartName("StrandSpecific");
  $scatter->setPlotTitle("$id");
  $scatter->setDefaultXMax(1);
  $scatter->setDefaultXMin(-1);
  $scatter->setDefaultYMax(1);
  $scatter->setDefaultYMin(-1);
  $scatter->setAntisenseFoldChange($antisenseFoldChange);
  $scatter->setSenseFoldChange($senseFoldChange);
  $scatter->setAdjustXYScalesTogether('TRUE');
  $self->setGraphObjects($scatter);

  return $self;
}
1;

# TEMPLATE_ANCHOR strandSpecificGraph

# package ApiCommonWebsite::View::GraphPackage::Templates::StrandSpecific::DS_7252b6506e;
# use base qw( ApiCommonWebsite::View::GraphPackage::Templates::StrandSpecific );
# use strict;

# sub getGraphType { 'bar' }
# sub excludedProfileSetsString { '' }
# sub getSampleLabelsString { '' }
# sub getColorsString { 'brown'  } 
# sub getForceXLabelsHorizontalString { 'true' } 
# sub getBottomMarginSize { 1 }
# sub getExprPlotPartModuleString { 'RNASeq' }
# sub getXAxisLabel { '${linePlotXAxisLabel}' }
# sub switchStrands {
#    if ('true' eq 'true') {
#      return 1;
#    } else {
#      return 0;
#    }

# }

# 1;
