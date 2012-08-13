package ApiCommonWebsite::View::GraphPackage::LegendPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;
#--------------------------------------------------------------------------------

sub getColors                  { $_[0]->{'_colors'                     }}
sub setColors                  { $_[0]->{'_colors'                     } = $_[1]}

sub getShortNames              { $_[0]->{'_short_names'                }}
sub setShortNames              { $_[0]->{'_short_names'                } = $_[1]}

sub getColumns                 { $_[0]->{'_columns'                    }}
sub setColumns                 { $_[0]->{'_columns'                    } = $_[1]}

sub getPointsPch               { $_[0]->{'_points_pch'                 }}
sub setPointsPch               { $_[0]->{'_points_pch'                 } = $_[1]}

sub getFill                    { $_[0]->{'_fill'                       }}
sub setFill                    { $_[0]->{'_fill'                       } = $_[1]}

sub getSize                    { $_[0]->{'_size'                       }}
sub setSize                    { $_[0]->{'_size'                       } = $_[1]; $_[0] }
 
#--------------------------------------------------------------------------------

sub new {
  my $class = shift;

  my $self = $class->SUPER::new(@_);
  my $id = $self->getId();

  return $self;
}

#--------------------------------------------------------------------------------

sub makeRPlotString {

  my ($self, $hash) = @_;

  my $colors = $self->getColors();
  my $names = $self->getShortNames();
  my $pch = 15 unless(defined $self->getPointsPch);
  my $fill = 1 unless(defined $self->getFill());
  my $nCols = $self->getColumns();

  print STDERR Dumper($colors);
  print STDERR Dumper($names);
  print STDERR Dumper($pch);
  print STDERR Dumper($fill);
  print STDERR Dumper($nCols);

  my $rColorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'legend.colors');
  my $rNamesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($names, 'legend.names');
  my $rPointsPchString = ApiCommonWebsite::View::GraphPackage::Util::rNumericVectorFromArray($pch, 'points.pch');
  my $rFill = $fill ? "TRUE" : "FALSE";

  $nCols = defined($nCols) ? $nCols : 2;

  my $rv = "
 #-------------------------------------------------------------------------------
 
  $rColorsString
  $rNamesString
  $rPointsPchString

  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs='i', xaxs='i', xaxt='n', yaxt='n', bty='n', mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type='l',col='orange', xlim=c(0,1),ylim=c(0,1));

  if($rFill) {
    legend(0.5, 0.5,
         legend.names,
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.9,
         ncol  = $nCols,
         fill=legend.colors,
         bty='n'
        );
  } else {
    legend(0.5, 0.5,
         legend.names,
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.9,
         pt.cex = 1.5,
         col   = legend.colors,
         pt.bg = legend.colors,
         pch   = points.pch,
         lty   = 'solid',
         ncol  = $nCols,
         bty='n'
        );
  }
";

}

1;
