package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;



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


#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR proteomicsSimpleNonRatio
