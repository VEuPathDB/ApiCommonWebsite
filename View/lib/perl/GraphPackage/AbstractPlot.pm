package ApiCommonWebsite::View::GraphPackage::AbstractPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );
use ApiCommonWebsite::View::GraphPackage;

use ApiCommonWebsite::Model::CannedQuery::ElementNames;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::View::MultiScreen;

use Data::Dumper;

#--------------------------------------------------------------------------------

sub getScreenSize                { $_[0]->{'_screen_size'                 }}
sub setScreenSize                { $_[0]->{'_screen_size'                 } = $_[1]; $_[0] }

sub getBottomMarginSize          { $_[0]->{'_bottom_margin_size'          }}
sub setBottomMarginSize          { $_[0]->{'_bottom_margin_size'          } = $_[1]; $_[0] }

sub getProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } }
sub setProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } = $_[1]; $_[0] }

sub getMultiScreen               { $_[0]->{'_multi_screen'                } }
sub setMultiScreen               { $_[0]->{'_multi_screen'                } = $_[1]; $_[0] }

sub getFileHandle                { $_[0]->{'_file_handle'                 } }
sub setFileHandle                { $_[0]->{'_file_handle'                 } = $_[1]; $_[0] }

sub getPlotWidth                 { $_[0]->{'_plot_width'                  } }
sub setPlotWidth                 { $_[0]->{'_plot_width'                  } = $_[1]; $_[0] }

sub getMainLegend                { $_[0]->{'_main_legend'                 }}
sub setMainLegend                { $_[0]->{'_main_legend'                 } = $_[1]; $_[0] }

sub getLegendSize                { $_[0]->{'_legend_size'                 }}
sub setLegendSize                { $_[0]->{'_legend_size'                 } = $_[1]; $_[0] }

sub getTempFiles                 { $_[0]->{'_temp_files'                  }}
sub setTempFiles                 { $_[0]->{'_temp_files'                  } = $_[1]; $_[0] }
sub addTempFile {
  my ($self, $file) = @_;

  push @{$self->getTempFiles()}, $file;
}

#--------------------------------------------------------------------------------
# Abstract methods
#--------------------------------------------------------------------------------

sub makeRPlotStrings {}

#--------------------------------------------------------------------------------

sub init {
  my ($self) = @_;

  $self->SUPER::init(@_);

  # Default 
  $self->setPlotWidth(400);
  $self->setLegendSize(40);

  $self->setTempFiles([]);

  $self;
}

#--------------------------------------------------------------------------------

sub makeRLegendString {
  my ($self) = @_;

  my $legendHash = $self->getMainLegend();

  my $colors = $legendHash->{colors};
  my $names = $legendHash->{short_names};
  my $pch = $legendHash->{points_pch};
  my $fill = $legendHash->{fill};
  my $nCols = $legendHash->{cols};

  my $rColorsString = $self->rStringVectorFromArray($colors, 'legend.colors');
  my $rNamesString = $self->rStringVectorFromArray($names, 'legend.names');
  my $rPointsPchString = $self->rNumericVectorFromArray($pch, 'points.pch');
  my $rFill = $fill ? "TRUE" : "FALSE";

  $nCols = defined($nCols) ? $nCols : 2;

  my $rv = "
 #-------------------------------------------------------------------------------
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  $rColorsString
  $rNamesString
  $rPointsPchString

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

#--------------------------------------------------------------------------------

sub makeR {
  my ($self, $rPlotStringsHash) = @_;

  my @rv;

  my $profileSetsHash = $self->getProfileSetsHash();

  my $thumb_b   = $self->getThumbnail();

  my $r_f = $self->getOutputFile(). '.R';
  my $r_fh = FileHandle->new(">$r_f") || die "Can not open R file '$r_f': $!";
  my $out_f     = $self->getOutputFile();

  $self->setFileHandle($r_fh);

  push(@rv, $r_f, $out_f);

  my $parts = [];
  if($self->getMainLegend()) {
    push(@$parts, { Name => "_LEGEND",   Size => $self->getLegendSize() });
  }

  foreach my $ps (keys %$profileSetsHash) {
    push(@$parts, { Name => "$ps",   Size => $self->getScreenSize() });
  }

  my $mS = ApiCommonWebsite::View::MultiScreen->new
    ( Parts => $parts,
      VisibleParts => $self->getVisibleParts(),
      Thumbnail    => $thumb_b
    );

  $self->setMultiScreen($mS);

  my $width       = $self->getPlotWidth();
  my $totalHeight = $mS->totalHeight();

  if ($thumb_b) {
    $width       *= 0.60;
    $totalHeight *= 0.60;
  }

  # used in R code to set locations of screens
  my $screens     = $mS->rScreenVectors();
  my $parts_n     = $mS->numberOfVisibleParts();

  my $open_R      = $self->rOpenFile($width, $totalHeight);
  my $preamble_R  = $self->_rStandardComponents($thumb_b);

  my $legend = "";
  my %isVis_b = $mS->partIsVisible();

  if($isVis_b{_LEGEND}) {
    $legend = $self->makeRLegendString();
  }

  my @rStrings = @{$self->makeRPlotStrings()};
  my $rStrings = join("\n", @rStrings);

  my $rcode =  <<RCODE;

# ------------------------------- Prepare --------------------------------

$preamble_R

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

ticks <- function() {
  axis(1, at=seq(x.min, x.max, 1), labels=F, col="gray75");
  axis(1, at=seq(5*floor(x.min/5+0.5), x.max, 5), labels=F, col="gray50");
  axis(1);
}

# --------------------------------- Add Legend-------------------------------

$legend

# --------------------------------- Add Plots ------------------------------

$rStrings


# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

RCODE


  print $r_fh $rcode;
#  print STDERR $rcode;

  $r_fh->close();

  my $tempFiles = $self->getTempFiles();

  push @rv, @$tempFiles;

  return @rv;
}

#--------------------------------------------------------------------------------

sub writeProfileFiles {
  my ($self, $profileSetName, $suffix, $elementOrder) = @_;

  my $_qh   = $self->getQueryHandle();
  my $_dict = {};

  my $r_fh = $self->getFileHandle();

  my $profile = ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => "_data_$suffix",
      Id           => $self->getId(),
      ProfileSet   => $profileSetName,
    );

  my $elementNames = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_names_$suffix",
        Id           => $self->getId(),
        ProfileSet   => $profileSetName,
      );

  my @errors;

  $profile->setElementOrder($elementOrder) if($elementOrder);
  $elementNames->setElementOrder($elementOrder) if($elementOrder);

  my $profile_fn = eval { $profile->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
  my $elementNames_fn = eval { $elementNames->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

  my @rv = ($profile_fn, $elementNames_fn);

  $self->addTempFile($profile_fn);
  $self->addTempFile($elementNames_fn);

  if (@errors) {
    $self->reportErrorsAndBlankGraph($r_fh, @errors);
  }

  return \@rv;
}


#--------------------------------------------------------------------------------

sub rStringVectorFromArray {
  my ($self, $stringArray, $name) = @_;

  return "$name = c(" . join(',', map {"\"$_\""} @$stringArray) . ");";
}

sub rNumericVectorFromArray {
  my ($self, $array, $name) = @_;

  return "$name = c(" . join(',', map {"$_"} @$array) . ");";
}


#--------------------------------------------------------------------------------





1;
