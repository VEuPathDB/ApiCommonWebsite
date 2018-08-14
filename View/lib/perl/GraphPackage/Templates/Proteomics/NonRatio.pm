package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

sub restrictProfileSetsBySourceId { return 1;}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::tgonME49_quantitativeMassSpec_Wastling_strain_timecourses_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio );
use strict;

sub setMainLegend {
  my $self = @_;

  my $colors = [ '#144BE5' , '#70598F' , '#5B984D' , '#FA9B83' , '#EF724E' , '#E1451A' ];

  print STDERR "Got here, but it's not working";
  my $pch = [ '15', '16', '17', '18', '7:10', '0:6'];

  my $legend = ['GT1 16 hr time course','ME49 16 hr time course', 'ME49 44 hr time course', 'RH 36 hr time course', 'VEG 16 hr time course', 'VEG 44 hr time course'];


  my $hash = {colors => [ '#E9967A', '#87CEFA', '#00BFFF','#4169E1', '#0000FF', ], short_names => $legend, points_pch => $pch, cols=> 2};

  $self->SUPER::setMainLegend($hash);
}

1;

# for ToxoDB


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_3c48f52edb;
use Data::Dumper;

sub getRemainderRegex {
  return qr/T\. ?gondii ?(.+) timecourse/;
}

sub keepSingleLegend {1}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setHasExtraLegend(1);
print STDERR Dumper($profile->getLegendLabels());
  if($profile->getLegendLabels()) {
    my @legendLabels = map {s/Quantitative protein expression of Tgondii proteins in infection of human cells - //;$_} @{$profile->getLegendLabels()};
    $profile->setLegendLabels(\@legendLabels);
    my $colorMap = "c(\"GT1 0 to 16 hour\" = \"#144BE5\", \"ME49 0 to 16 hour\" = \"#70598F\", \"ME49 0 to 44 hour\" = \"#5B984D\", \"RH 0 to 36 hour\" = \"#FA9B83\", \"VEG 0 to 16 hour\" = \"#EF724E\", \"VEG 0 to 44 hour\" = \"#E1451A\")";

    $profile->setColorVals($colorMap);
  }

  return $self;
}
1;


# for HostDB
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_08fe07cd15;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $legend = ['GT1 0 to 16 hr','ME49 0 to 16 hr','ME49 0 to 44 hr', 'RH 0 to 36 hr',
		'VEG 0 to 16 hr', 'VEG 0 to 44 hr'];
  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legend);
  return $self;
}
1;


# for TriTrypDB
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_bf9c234fd9;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setDefaultYMax(0.4);

  my @allLegend;
  foreach(1..2) {
    push @allLegend, "G1";
  }
  foreach(1..2) {
    push @allLegend, "S";
  }
  foreach(1..3) {
    push @allLegend, "G2";
  }
  foreach(1..2) {
    push @allLegend, "G1";
  }
  $profile->setColors(['#aed6f1','#a9dfbf', '#f9e79f' ]);

  $profile->setHasExtraLegend(1); 
  $profile->setLegendLabels(\@allLegend);


  my $plotTitle = $profile->getPlotTitle();
  $profile->setPlotTitle($plotTitle . " : Cell cycle phases" );

  return $self;
}

1;




#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR proteomicsSimpleNonRatio
