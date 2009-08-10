
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::View::MultiScreen;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use Time::HiRes qw ( time );

sub init {
   my $Self = shift;
   my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getDataNamesQuery       { $_[0]->{'DataNamesQuery'       } }
sub setDataNamesQuery       { $_[0]->{'DataNamesQuery'       } = $_[1]; $_[0] }

sub getDataQuery            { $_[0]->{'DataQuery'            } }
sub setDataQuery            { $_[0]->{'DataQuery'            } = $_[1]; $_[0] }

sub getDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } }
sub setDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } = $_[1]; $_[0] }

sub getDataColors           { $_[0]->{'DataColors'           } }
sub setDataColors           { $_[0]->{'DataColors'           } = $_[1]; $_[0] }

sub getPctNamesQuery        { $_[0]->{'PctNamesQuery'        } }
sub setPctNamesQuery        { $_[0]->{'PctNamesQuery'        } = $_[1]; $_[0] }

sub getPctQuery             { $_[0]->{'PctQuery'             } }
sub setPctQuery             { $_[0]->{'PctQuery'             } = $_[1]; $_[0] }

sub getPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } }
sub setPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } = $_[1]; $_[0] }

sub getPctColors            { $_[0]->{'PctColors'            } }
sub setPctColors            { $_[0]->{'PctColors'            } = $_[1]; $_[0] }

sub getPctIsDecimal         { $_[0]->{'PctIsDecimal'         } }
sub setPctIsDecimal         { $_[0]->{'PctIsDecimal'         } = $_[1]; $_[0] }

sub getTagRx                { $_[0]->{'TagRx'                } }
sub setTagRx                { $_[0]->{'TagRx'                } = $_[1]; $_[0] }

# ========================================================================
# ------------------------------- Methods --------------------------------
# ========================================================================

sub makeR {
   my $Self = shift;

   my @Rv;

   my $id = $Self->getId();

   my $_qh   = $Self->getQueryHandle();
   my $_dict = {};

   my $thumb_b   = $Self->getThumbnail();

   my $fmt       = $Self->getFormat();
   my $r_f       = $Self->getOutputFile(). '.R';
   my $out_f     = $Self->getOutputFile();

   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors = ();

   my $_names = eval { $Self->getDataNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_data  = eval { $Self->getDataQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   my $_pct_names = eval { $Self->getPctNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_pct_data  = eval { $Self->getPctQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);


   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my @tags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_names;
      my @avg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_data;

      my @pctTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_pct_names;
      my @pctAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_pct_data;

      if($Self->getPctIsDecimal()) {
        @pctAvg  = map {$_ * 100 } @pctAvg;
      }

      my @std  = (0) x scalar @tags;

      my $tags   = join(', ', map { "'$_'" } @tags);
      my $avg    = join(', ', @avg);
      my $ylab   = $Self->getDataYaxisLabel();
      my $colors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @{$Self->getDataColors()});

      my $pctTags  = join(', ', map { "'$_'" } @pctTags);
      my $pctAvg    = join(', ', @pctAvg);
      my $pctYlab   = $Self->getPctYaxisLabel();
      my $pctColors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @{$Self->getPctColors()});

      my $std    = join(', ', @std);

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'hist',   Size => 210 },
                   { Name => 'pct',   Size => 180 },
                 ],
        VisibleParts => $Self->getVisibleParts(),
        Thumbnail    => $thumb_b
      );

      # used in R code to branch
      my %isVis_b     = $_mS->partIsVisible();

      my $width       = 450;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width       *= 0.75;
         $totalHeight *= 0.75;
      }

      # used in R code to set locations of screens
      my $screens     = $_mS->rScreenVectors();

      my $parts_n     = $_mS->numberOfVisibleParts();
      my $open_R      = $Self->rOpenFile($width, $totalHeight);
      my $preamble_R  = $Self->_rStandardComponents($thumb_b);

      print $r_fh <<R;

# ------------------------------- Prepare --------------------------------

$preamble_R

# ---------------------------- Load Data Sets ----------------------------

the.tags        <- c($tags);
the.avg         <- c($avg);
the.colors      <- c($colors);

pct.avg         <- c($pctAvg);
pct.tags        <- c($pctTags);
pct.colors      <- c($pctColors);

the.std         <- c($std);

# --------------------------- Prepare To Plot ----------------------------

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4, $parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

ticks <- function() {
  axis(1, at=seq(x.min, x.max, 1), labels=F, col="gray75");
  axis(1, at=seq(5*floor(x.min/5+0.5), x.max, 5), labels=F, col="gray50");
  axis(1);
}

# -------------------- SCREEN 2 : Histogram Data Plot --------------------

if ($isVis_b{hist} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1));

  d.min = min(1.1 * (the.avg - the.std), 0);
  d.max = max(1.1 * (the.avg + the.std), 0);

  d.min.default = -1;
  d.max.default = 1;

  if(d.min > d.min.default) {
    d.min = d.min.default;
  }

  if(d.max < d.max.default) {
    d.max = d.max.default;
  }

  c <- barplot(the.avg,
               col       =the.colors,
               ylab      = '$ylab',
               ylim      = c(d.min, d.max),
                names.arg = the.tags,
               las       = 2
              );

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(c, the.avg - the.std, c, the.avg+the.std,
                           col="black",
                           lw=2,
                           angle=90,
                           code=3,
                           length=0.05
                           )
                   );
  box();
}

# -------------------- SCREEN 2 : Percentile Histogram Data Plot --------------------

if ($isVis_b{hist} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1));

  c <- barplot(pct.avg,
               col       = pct.colors,
               ylab      = '$pctYlab',
               ylim      = c(0, 100),
                names.arg = pct.tags,
               las = 2
              );

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(c, pct.avg - the.std, c, pct.avg+the.std,
                           col="black",
                           lw=2,
                           angle=90,
                           code=3,
                           length=0.05
                           )
                   );
  box();
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f );

   return @Rv;
}

1;

