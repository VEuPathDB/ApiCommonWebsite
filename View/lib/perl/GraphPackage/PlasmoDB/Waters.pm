
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Waters;

=pod

=head1 Summary

Plots data from the Waters study of I<Plasmodium berghei>.

There are two data types (average log ratios and average percentages),
two strains, and 4 experimental conditions (MS, R, YS, YT).

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
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;
	 my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

	 $Self->setShortNamesQuery      ( $Args->{ShortNamesQuery     } );
   $Self->setPercentageHpQuery    ( $Args->{PercentageHpQuery   } );
   $Self->setLogRatioHpQuery      ( $Args->{LogRatioHpQuery     } );
   $Self->setPercentageHpeQuery   ( $Args->{PercentageHpeQuery  } );
   $Self->setLogRatioHpeQuery     ( $Args->{LogRatioHpeQuery    } );

   return $Self;
}

# ========================================================================
# ----------------------- Symbolic Access to Data ------------------------
# ========================================================================

sub getShortNamesQuery      { $_[0]->{'ShortNamesQuery'           } }
sub setShortNamesQuery      { $_[0]->{'ShortNamesQuery'           } = $_[1]; $_[0] }

sub getPercentageHpQuery    { $_[0]->{'PercentageHpQuery'           } }
sub setPercentageHpQuery    { $_[0]->{'PercentageHpQuery'           } = $_[1]; $_[0] }

sub getLogRatioHpQuery      { $_[0]->{'LogRatioHpQuery'             } }
sub setLogRatioHpQuery      { $_[0]->{'LogRatioHpQuery'             } = $_[1]; $_[0] }

sub getPercentageHpeQuery   { $_[0]->{'PercentageHpeQuery'          } }
sub setPercentageHpeQuery   { $_[0]->{'PercentageHpeQuery'          } = $_[1]; $_[0] }

sub getLogRatioHpeQuery     { $_[0]->{'LogRatioHpeQuery'            } }
sub setLogRatioHpeQuery     { $_[0]->{'LogRatioHpeQuery'            } = $_[1]; $_[0] }

# ========================================================================
# ------------------------------- Methods --------------------------------
# ========================================================================

sub makeR {
   my $Self = shift;

   my @Rv;

   my $id    = $Self->getId();

   my $_qh   = $Self->getQueryHandle();
   my $_dict = {};

	 my $fmt       = $Self->getFormat();

   my $thumb_b   = $Self->getThumbnail();

   my $minLgRat  = 1;
   my $maxLgRat  = 10;

	 my @_names    = $Self->getShortNamesQuery()->getValues($_qh, $_dict); #qw( MS R YS YT );
	 my $names     = join(',', map { '"'. $_->{NAME}. '"' } @_names);
	 my $names_n   = scalar @_names;

   my $r_f  = $Self->getOutputFile(). '.R';
   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors;

   my $lgrHp_f  = eval { $Self->getLogRatioHpQuery(   )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $pctHp_f  = eval { $Self->getPercentageHpQuery( )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $lgrHpe_f = eval { $Self->getLogRatioHpeQuery(  )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $pctHpe_f = eval { $Self->getPercentageHpeQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'LEGEND', Size => 40  },
                   { Name => 'rat',    Size => 240 },
                   { Name => 'pct',    Size => 120 },
                 ],
        VisibleParts  => $Self->getVisibleParts(),
        Thumbnail     => $thumb_b,
      );

      # used in R code to branch
      my %isVis_b = $_mS->partIsVisible();

      my $width = 480;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width        = 250;
         $totalHeight *= 0.8;
      }

      # used in R code to set locations of screens
      my $screens    = $_mS->rScreenVectors();
      my $parts_n    = $_mS->numberOfVisibleParts();

      my $open_R     = $Self->rOpenFile($width, $totalHeight);
      my $preamble_R = $Self->_rStandardComponents($thumb_b);

      print $r_fh <<R;

# ------------------------------ Libraries -------------------------------

$preamble_R

# ------------------------------ Constants -------------------------------

cutoff.exp <- 10;
cutoff.lgp <- -0.5;

# Steve, change colors here.
color.strain.hp  <- rgb(153,   0,   0, max=255);
color.strain.hpe <- rgb(  0,   0, 153, max=255);

# ------------------------------ Load Data -------------------------------

data.names     <- c($names);

data.lgr.hp    <- read.table("$lgrHp_f",  header=T);
data.pct.hp    <- read.table("$pctHp_f",  header=T);
data.lgr.hpe   <- read.table("$lgrHpe_f", header=T);
data.pct.hpe   <- read.table("$pctHpe_f", header=T);

# ----------------------------- Data Limits ------------------------------

ylim.lgr      <- c(max(-1*$maxLgRat,min(-1*$minLgRat,min(data.lgr.hp\$VALUE, data.lgr.hpe\$VALUE))),
                   min(   $maxLgRat,max(   $minLgRat,max(data.lgr.hp\$VALUE, data.lgr.hpe\$VALUE)))
                  );

data.lgr      <- t(matrix(c(data.lgr.hp\$VALUE, data.lgr.hpe\$VALUE), ncol=2));
data.pct      <- t(matrix(c(data.pct.hp\$VALUE, data.pct.hpe\$VALUE), ncol=2));

# --------------------------- Prepare To Plot ----------------------------

# open output file; set general plotting parameters; get ready to make
# three subplots.

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screen.i    <- 1;

# ------------------ SCREEN : Legend -----------------------------------

if ($isVis_b{LEGEND} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs="i", xaxs="i", xaxt="n", yaxt="n", bty="n", mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type="l",col="orange", xlim=c(0,1),ylim=c(0,1));

  legend(0.5, 0.5,
         c("HP Strain", "HPE Strain"),
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.90,
         bty   = "n",
         lty   = "solid",
         lw    = 5,
         col   = c(color.strain.hp, color.strain.hpe),
         horiz = T
        );
}


# ----------------- SCREEN : Induction and Repression ------------------

# plot points for both data sets, draw line at 0-induction, draw
# connecting lines, add X-axis line.

if ($isVis_b{rat} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  barplot(data.lgr,
          beside    = 1,
          space     = c(0.1, 1),
          ylab      = "log2(Exp/Avg)",
          names.arg = c($names),
          col       = c(color.strain.hp, color.strain.hpe),
          border    = c(color.strain.hp, color.strain.hpe),
          ylim      = ylim.lgr,
          axis.lty  = "solid"
         );

  #axis(1,at=seq(1,4),labels=data.names,tick=T);
  plasmodb.grid(nx=NA,ny=NULL);
  lines (c(-100,100), c(0,0), col="gray25");
  plasmodb.title("Induction/Repression");
}

# ---------------------- SCREEN : Percentage Plot ----------------------

# do bar plot, try to get x-axis line.

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;
  barplot(data.pct,
          beside    = 1,
          space     = c(0.1, 1),
          ylab      = "%",
          names.arg = data.names,
          col       = c(color.strain.hp, color.strain.hpe),
          border    = c(color.strain.hp, color.strain.hpe),
          ylim      = c(0,100),
          axis.lty  = "solid"
         );
  plasmodb.title("Expression levels (percentiled)");
}

# --------------------------------- Done ---------------------------------

# close the graphics file and quit.

dev.off();

quit(save="no")

R
   }

   $r_fh->close();

   push(@Rv, $r_f, $lgrHp_f, $pctHp_f, $lgrHpe_f, $pctHpe_f);

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
