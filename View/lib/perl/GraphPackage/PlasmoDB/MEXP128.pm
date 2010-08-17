
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

sub getRedPctNamesQuery        { $_[0]->{'RedPctNamesQuery'        } }
sub setRedPctNamesQuery        { $_[0]->{'RedPctNamesQuery'        } = $_[1]; $_[0] }

sub getRedPctQuery             { $_[0]->{'RedPctQuery'             } }
sub setRedPctQuery             { $_[0]->{'RedPctQuery'             } = $_[1]; $_[0] }

sub getGreenPctNamesQuery        { $_[0]->{'GreenPctNamesQuery'        } }
sub setGreenPctNamesQuery        { $_[0]->{'GreenPctNamesQuery'        } = $_[1]; $_[0] }

sub getGreenPctQuery             { $_[0]->{'GreenPctQuery'             } }
sub setGreenPctQuery             { $_[0]->{'GreenPctQuery'             } = $_[1]; $_[0] }

sub getPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } }
sub setPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } = $_[1]; $_[0] }

sub getPctColors            { $_[0]->{'PctColors'            } }
sub setPctColors            { $_[0]->{'PctColors'            } = $_[1]; $_[0] }

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

   my $_red_pct_names = eval { $Self->getRedPctNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_red_pct_data  = eval { $Self->getRedPctQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   my $_green_pct_names = eval { $Self->getGreenPctNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_green_pct_data  = eval { $Self->getGreenPctQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);


   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my @tags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_names;
      my @avg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_data;

      my @redPctTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_red_pct_names;
      my @redPctAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_red_pct_data;

      my @greenPctTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_green_pct_names;
      my @greenPctAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_green_pct_data;

      my @std  = (0) x scalar @tags;

      my $tags   = join(', ', map { "'$_'" } @tags);
      my $avg    = join(', ', @avg);
      my $ylab   = $Self->getDataYaxisLabel();
      my $colors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @{$Self->getDataColors()});

      my $redPctTags  = join(', ', map { "'$_'" } @redPctTags);
      my $redPctAvg    = join(', ', @redPctAvg);

      my $greenPctTags  = join(', ', map { "'$_'" } @greenPctTags);
      my $greenPctAvg    = join(', ', @greenPctAvg);


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

red.pct.avg         <- c($redPctAvg);
red.pct.tags        <- c($redPctTags);

green.pct.avg         <- c($greenPctAvg);
green.pct.tags        <- c($greenPctTags);

pct.colors      <- c('#A52A2A', '#FFDAB9');

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

  c <- barplot(rbind(red.pct.avg,green.pct.avg),
               col       = pct.colors,
               ylab      = '$pctYlab',
               ylim      = c(0, 100),
                names.arg = red.pct.tags,
               las = 2,
               beside=TRUE
              );

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(c, red.pct.avg - the.std, c, red.pct.avg+the.std,
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

