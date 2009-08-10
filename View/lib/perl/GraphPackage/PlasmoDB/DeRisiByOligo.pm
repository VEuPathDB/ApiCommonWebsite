
package PlasmoDBWebsite::View::GraphPackage::DeRisiByOligo;

=pod

=head1 Summary

Makes the plots for the deRisi developmental time series experiments.
The data consists of four parts:

=over

=item Normalized and Smoothed Data

This part is called C<ns> and contains the log(red/green) normalized
data and the same data smoothed over multiple time points.

=item Raw Red and Green Data

This part is called C<raw> and contains the raw red and green
channels.

=item Percents

This part is called C<pct> and contains the percentile ranks of the
normalized levels.

=item Life Stage Fractions

This part is called C<lsf> and contains the life-stage fractions.

=back

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::View::MultiScreen;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use Time::HiRes qw ( time );

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;
   my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

   $Self->setSmoothQuery          ( $Args->{SmoothQuery         } );
   $Self->setRoughQuery           ( $Args->{RoughQuery          } );
	 $Self->setRedQuery             ( $Args->{RedQuery            } );
	 $Self->setGreenQuery           ( $Args->{GreenQuery          } );
   $Self->setPercentQuery         ( $Args->{PercentQuery        } );
   $Self->setLifeStageQuery       ( $Args->{LifeStageQuery      } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getSmoothQuery          { $_[0]->{'SmoothQuery'       } }
sub setSmoothQuery          { $_[0]->{'SmoothQuery'       } = $_[1]; $_[0] }

sub getRoughQuery           { $_[0]->{'RoughQuery'        } }
sub setRoughQuery           { $_[0]->{'RoughQuery'        } = $_[1]; $_[0] }

sub getRedQuery             { $_[0]->{'RedQuery'          } }
sub setRedQuery             { $_[0]->{'RedQuery'          } = $_[1]; $_[0] }

sub getGreenQuery           { $_[0]->{'GreenQuery'        } }
sub setGreenQuery           { $_[0]->{'GreenQuery'        } = $_[1]; $_[0] }

sub getPercentQuery         { $_[0]->{'PercentQuery'      } }
sub setPercentQuery         { $_[0]->{'PercentQuery'      } = $_[1]; $_[0] }

sub getLifeStageQuery       { $_[0]->{'LifeStageQuery'    } }
sub setLifeStageQuery       { $_[0]->{'LifeStageQuery'    } = $_[1]; $_[0] }

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

   my $smooth_f    = eval { $Self->getSmoothQuery()   ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $rough_f     = eval { $Self->getRoughQuery()    ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $redRaw_f    = eval { $Self->getRedQuery()      ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $greenRaw_f  = eval { $Self->getGreenQuery()    ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $percent_f   = eval { $Self->getPercentQuery()  ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $lifestage_f = eval { $Self->getLifeStageQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'ns',  Size => 200 },
                   { Name => 'raw', Size => 200 },
                   { Name => 'pct', Size => 150 },
                   { Name => 'lsf', Size => 100 },
                 ],
        VisibleParts => $Self->getVisibleParts(),
        Thumbnail    => $thumb_b
      );

      # used in R code to branch
      my %isVis_b     = $_mS->partIsVisible();

      my $width       = 480;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width        = 250;
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

data.smooth    <- read.table("$smooth_f",    header=T);
data.rough     <- read.table("$rough_f",     header=T);
data.red.raw   <- read.table("$redRaw_f",    header=T);
data.green.raw <- read.table("$greenRaw_f",  header=T);
data.percent   <- read.table("$percent_f",   header=T);
data.life      <- read.table("$lifestage_f", header=T);

# --------------------------- Prepare To Plot ----------------------------

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

x.min <- min(data.smooth\$ELEMENT_ORDER);
x.max <- max(data.smooth\$ELEMENT_ORDER);
x.del <- x.max - x.min;
x.min <- x.min - x.del / 30;
x.max <- x.max + x.del / 30;

# make room for large y-axis labels for raw values
set.margins <- function () { };

# ------------------ SCREEN 1 : Rough and Smooth Plots -------------------

# switch to screen 1, plot rough data, plot smooth data, draw finer
# ticks, draw grid.

if ($isVis_b{ns} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  set.margins();

  plot(data.rough\$ELEMENT_ORDER,
       data.rough\$VALUE,
       col  = "gray",
       bg   = "gray",
       type = "o",
       pch  = 22,
       xlab = "",
       xlim = c(x.min, x.max),
       ylab = "lg(Cy5/Cy3)",
       ylim = c(min(-2, data.rough\$VALUE),
                max( 2, data.rough\$VALUE)
               )
      );
  lines(data.smooth\$ELEMENT_ORDER,
        data.smooth\$VALUE,
        col  = "blue",
        bg   = "blue",
        type = "o",
        pch  = 22
       );
  plasmodb.ticks(1,0,53,5);
  plasmodb.grid();
  plasmodb.title("Induction/Repression");
}

# ----------------------- SCREEN 2 : Raw Data Plot -----------------------

if ($isVis_b{raw} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  ylim.min = min(-2, data.green.raw\$VALUE, data.red.raw\$VALUE);
  ylim.max = max( 2, data.green.raw\$VALUE, data.red.raw\$VALUE);

  par(cex.axis = 0.8);

  plot(data.green.raw\$ELEMENT_ORDER,
       data.green.raw\$VALUE,
       col  = "green",
       bg   = "green",
       type = "o",
       pch  = 22,
       xlab = "Time Point",
       xlim = c(x.min, x.max),
       ylab = "Raw Expression Value",
       ylim = c(ylim.min, ylim.max)
      );
  lines(data.red.raw\$ELEMENT_ORDER,
        data.red.raw\$VALUE,
        col  = "red",
        bg   = "red",
        type = "o",
        pch  = 22
       );
  plasmodb.ticks(1, 0, nrow(data.red.raw), 5);
  plasmodb.grid();
  plasmodb.title("Raw Values");

  par(cex.axis = 1);
}

# ---------------------- SCREEN 3 : Percentile Data ----------------------

# switch to screen 2, plot percentile data, nice ticks, and a box

#data.percent.x = c(x.min,x.max);
#data.percent.y = c(0,    1);

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxp   = c(0,100,1));
  set.margins();
  plot(data.percent\$ELEMENT_ORDER, # data.percent.x,
       100*data.percent\$VALUE,     # data.percent.y,
       type   = "l",
       col    = plasmodb.pct.color,
       xlab   = "",
       xlim   = c(x.min, x.max),
       ylim   = c(0,100),
       ylab   = "%"
      );
  plasmodb.filled.plot(data.percent\$ELEMENT_ORDER,
                       100*data.percent\$VALUE,
                       border = plasmodb.pct.color,
                       col    = plasmodb.pct.color
                      );
  plasmodb.ticks(1,0,53,5);
  plasmodb.title("Expression levels (percentiled)");
}

# ------------------- SCREEN 4 : Life Stage Fractions --------------------

if ($isVis_b{lsf} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  set.margins();

  # this is a dummy just to get the plot established
  plot (c(0), c(0),
        type = "l",
        xlab = "Time Points",
        xlim = c(x.min, x.max),
        ylab = "%",
        ylim = c(0,100),
        col  = rgb(1,0.5,0.5),
        lwd  = 3,
        cex.axis = 0.75
       );

  # filled plots
  plasmodb.filled.plot(data.life\$ELEMENT_ORDER,
                       data.life\$Ring,
                       border = plasmodb.ring.color,
                       col    = plasmodb.ring.color
                      );

  plasmodb.filled.plot(data.life\$ELEMENT_ORDER,
                       data.life\$Schizont,
                       border = plasmodb.schizont.color,
                       col    = plasmodb.schizont.color
                      );

  plasmodb.filled.plot(data.life\$ELEMENT_ORDER,
                       data.life\$Trophozoite,
                       border = plasmodb.trophozoite.color,
                       col    = plasmodb.trophozoite.color
                      );

  # outlines
  lines(data.life\$ELEMENT_ORDER, data.life\$Ring,        lwd=3, col=plasmodb.ring.color );
  lines(data.life\$ELEMENT_ORDER, data.life\$Schizont,    lwd=3, col=plasmodb.schizont.color );
  lines(data.life\$ELEMENT_ORDER, data.life\$Trophozoite, lwd=3, col=plasmodb.trophozoite.color );

  # text labels
  text(8,  50, col="white", labels=c("Ring"));
  text(25, 50, col="white", labels=c("Trophozoite"));
  text(40, 50, col="white", labels=c("Schizont"));

  plasmodb.ticks(1,0,53,5);
  plasmodb.title("Life Stage Population Percentages");
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f,
        $smooth_f, $rough_f,
        $redRaw_f, $greenRaw_f,
        $percent_f, $lifestage_f
       );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
