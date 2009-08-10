
package PlasmoDBWebsite::View::GraphPackage::DeRisi;

=pod

=head1 Summary

Makes the plots for the deRisi developmental time series experiments.
The data consists of two profiles showing the rough and smoothed
log-ratio data, and a third profile containing the percentile of the
rough data.

The data is shown in two plots.  The first plot contains the rough and
smooth log-ratio data.  The second plot contains the percentile data
for the smooth data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );

use ApiCommonWebsite::View::GraphPackage;
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
   $Self->setPercentQuery         ( $Args->{PercentQuery        } );
   $Self->setLifeStageQuery       ( $Args->{LifeStageQuery      } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getSmoothQuery          { $_[0]->{'SmoothQuery'       } }
sub setSmoothQuery          { $_[0]->{'SmoothQuery'       } = $_[1]; $_[0] }

sub getRoughQuery           { $_[0]->{'RoughQuery'        } }
sub setRoughQuery           { $_[0]->{'RoughQuery'        } = $_[1]; $_[0] }

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

   my $fmt       = $Self->getFormat();
   my $r_f       = $Self->getOutputFile(). '.R';
   my $out_f     = $Self->getOutputFile();

   my @errors = ();

   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my $smooth_f    = eval { $Self->getSmoothQuery()   ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $rough_f     = eval { $Self->getRoughQuery()    ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $percent_f   = eval { $Self->getPercentQuery()  ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $lifestage_f = eval { $Self->getLifeStageQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $preamble_R = $Self->_rStandardComponents();
      my $open_R    = $Self->rOpenFile(480,480);

      print $r_fh <<R;

# ------------------------------- Prepare --------------------------------

$preamble_R

# ---------------------------- Load Data Sets ----------------------------

data.smooth  <- read.table("$smooth_f",    header=T);
data.rough   <- read.table("$rough_f",     header=T);
data.percent <- read.table("$percent_f",   header=T);
data.life    <- read.table("$lifestage_f", header=T);

# --------------------------- Prepare To Plot ----------------------------

$open_R;

plasmodb.par(xaxs="i");

screen.dims <- t(array(c(c(0.0, 1.0, 0.50, 1.00),
                         c(0.0, 1.0, 0.25, 0.50),
                         c(0.0, 1.0, 0.00, 0.25)
                        ),
                       dim=c(4,3)
                       )
                 );
screens     <- split.screen(screen.dims, erase=T);

x.min <- min(data.smooth\$ELEMENT_ORDER);
x.max <- max(data.smooth\$ELEMENT_ORDER);
x.del <- x.max - x.min;
x.min <- x.min - x.del / 30;
x.max <- x.max + x.del / 30;

# ------------------ SCREEN 1 : Rough and Smooth Plots -------------------

# switch to screen 1, plot rough data, plot smooth data, draw finer
# ticks, draw grid.

screen(screens[1]);
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

# ---------------------- SCREEN 2 : Percentile Data ----------------------

# switch to screen 2, plot percentile data, nice ticks, and a box

#data.percent.x = c(x.min,x.max);
#data.percent.y = c(0,    1);

screen(screens[2]);
par(yaxp   = c(0,100,1));
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

#text(x.max, 50, "need average percents", col="white", adj=1);

# ----------------------------- Life Stages ------------------------------

screen(screens[3]);
plasmodb.par.last();

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

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f, $smooth_f, $rough_f, $percent_f, $lifestage_f);

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
