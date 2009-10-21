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

sub getProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } }
sub setProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } = $_[1]; $_[0] }

sub getMultiScreen               { $_[0]->{'_multi_screen'                } }
sub setMultiScreen               { $_[0]->{'_multi_screen'                } = $_[1]; $_[0] }

sub getFileHandle                { $_[0]->{'_file_handle'                 } }
sub setFileHandle                { $_[0]->{'_file_handle'                 } = $_[1]; $_[0] }

sub getPlotWidth                 { $_[0]->{'_plot_width'                  } }
sub setPlotWidth                 { $_[0]->{'_plot_width'                  } = $_[1]; $_[0] }

#--------------------------------------------------------------------------------
# Abstract methods
#--------------------------------------------------------------------------------

sub makeRPlotStrings {}

#--------------------------------------------------------------------------------

sub init {
  my ($self) = @_;

  $self->SUPER::init(@_);

  # Default 
  $self->setPlotWidth(600);

  $self;
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

  push(@rv, $r_f, $out_f);

  my $parts = [];
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
    $width       *= 0.75;
    $totalHeight *= 0.75;
  }

  # used in R code to set locations of screens
  my $screens     = $mS->rScreenVectors();
  my $parts_n     = $mS->numberOfVisibleParts();

  my $open_R      = $self->rOpenFile($width, $totalHeight);
  my $preamble_R  = $self->_rStandardComponents($thumb_b);

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

# --------------------------------- Add Plots ---------------------------------

$rStrings


# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

RCODE


  print $r_fh $rcode;
#  print STDERR $rcode;

  $r_fh->close();

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


#--------------------------------------------------------------------------------





1;
