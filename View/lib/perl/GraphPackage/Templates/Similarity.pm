package ApiCommonWebsite::View::GraphPackage::Templates::Similarity;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::SimilarityPlot;

use ApiCommonWebsite::View::GraphPackage::Util;
#use Data::Dumper;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my @colors = ('blue', 'grey');
  my @legend = ('Match', 'Query');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  # Need to make 2 Profiles ... one for the primaryID and one for the Secondary
  my $profile = $self->getProfile;
  my @profileArray = (["$profile","values"],
                      ["$profile","values"],
                     );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $similarity = ApiCommonWebsite::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets($profileSets);
  $similarity->setColors(\@colors);
  $similarity->setPointsPch([15,15]);
  $similarity->setElementNameMarginSize(6.5);
  $similarity->setXaxisLabel($self->getXAxisLabel());
  $self->setGraphObjects($similarity);

  return $similarity;
}


sub getProfile {
 return ;
}

1;

# TEMPLATE_ANCHOR similarityGraph


#cneoH99_Haase_Kelliher_Cell_Cycle_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_4b0e1b490a;
sub getProfile {
  my ($self) = @_;
  my $pset = 'C. neoformans cell-cycle RNAseq [htseq-union - firststrand - fpkm]';

  return $pset;
}

1;

# scerS288c_microarrayExpression_Spellman_CellCycle_1998_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_f101fb2669;
sub getProfile {
  my ($self) = @_;
  my $pset = 'S. cerevisiae cell-cycle RNAseq [htseq-union - firststrand - fpkm]';

  return $pset;
}
1;

