package ApiCommonWebsite::View::GraphPackage::CryptoDB::Kissinger::KissingerRtPcrProfiles;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(8);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'];

  my $xAxisLabels = ['2 Hrs', '6 Hrs','12 Hrs', '24 Hrs','36 Hrs','48 Hrs','72 Hrs'];


  $self->setProfileSetsHash
    ({'rma' => {profiles => ['Cparvum_RT_PCR_Kissinger'],
                           y_axis_label => 'Median Expr (standardized)',
                           default_y_max => 1,
                           colors => $colors,
                           plot_title => 'C.parvum semi-quant. Real Time PCR Expr. Profiles',
                           x_axis_labels => $xAxisLabels,
                          }
    });

  return $self;
}

1;
