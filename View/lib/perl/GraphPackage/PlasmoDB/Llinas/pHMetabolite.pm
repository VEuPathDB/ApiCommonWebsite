package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Llinas::pHMetabolite;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  my $colors = ['dodgerblue', 'salmon', 'forestgreen', '#6495ED', '#E9967A', '#2F4F4F' ];
  my $legend = ['Percoll pellet', 'RBC pellet', 'Saponin pelllet', 'Percoll media', 'RBC media', 'Saponin media'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3 });

my $percollPellet = ['','pH 6.4','pH 7.4','pH 8.4','','','','','','','','','','','','','','',''];
my $rbcPellet     = ['','','','','pH 6.4','pH 7.4','pH 8.4','','','','','','','','','','','',''];
my $saponinPellet = ['','','','','','','','pH 6.4','pH 7.4','pH 8.4','','','','','','','','',''];
my $percolMedia   = ['','','','','','','','','','','','pH 6.4','pH 7.4','pH 8.4','','','','',''];
my $rbcMedia      = ['','','','','','','','','','','','','','','pH 6.4','pH 7.4','pH 8.4','',''];
my $saponinMedia  = ['','','','','','','','','','','','','','','','','pH 6.4','pH 7.4','pH 8.4'];


  my @profileSetNames = (['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $percollPellet],
			 ['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $rbcPellet],
			 ['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $saponinPellet],
			 ['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $percolMedia],
			 ['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $rbcMedia],
			 ['Profiles of Metabolites from Llinas','standard error - Profiles of Metabolites from Llinas', $saponinMedia]
			);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $massSpec = ApiCommonWebsite::View::GraphPackage::BarPlot::MassSpec->new(@_);
  $massSpec->setProfileSets($profileSets);
  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);
#  $massSpec->setDefaultYMin(-1);

  $massSpec->setElementNameMarginSize(4);
#  $massSpec->setScreenSize(300); # to increase height of graph

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
