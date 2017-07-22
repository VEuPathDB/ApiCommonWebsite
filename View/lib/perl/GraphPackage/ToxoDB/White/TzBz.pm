package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $legend = ["GT1", "ME49", "CTGara"];

  my $colors = ['#B22222', '#6A5ACD', '#87CEEB' ];
  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});



  my $gt1Samples = ['Tachyzoite', 'Compound 1', 'pH=8.2','','','','','',''];
  my $me49Samples = ['','','','Tachyzoite', 'Compound 1', 'pH=8.2','','',''];
  my $ctgaraSamples = ['','','', '','','', 'Tachyzoite', 'Compound 1', 'pH=8.2'];


  my @profileSetsArray = (['three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           'standard error - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           $gt1Samples],
                          ['three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           'standard error - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           $me49Samples],
                          ['three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           'standard error - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', 
                           $ctgaraSamples],
                          );

  my @percentileSetsArray = (['percentile - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', '',$gt1Samples],
                             ['percentile - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', '',$me49Samples],
                             ['percentile - three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions', '',$ctgaraSamples],
                             );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);


  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile);

  return $self;

}



1;
