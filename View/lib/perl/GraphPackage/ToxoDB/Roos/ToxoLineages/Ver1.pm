package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::ToxoLineages::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);

  my $colors = ['#4682B4', '#B22222', '#8FBC8F', '#6A5ACD', '#87CEEB', '#CD853F'];

  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiling of the 3 archetypal T. gondii lineages'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
              plot_title => 'Tachyzoite comparison of archetypal T.gondii lineages',
             },
     });

  return $self;
}



1;
