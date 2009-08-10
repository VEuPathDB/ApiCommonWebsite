
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::WbcGametocytes;

=pod

=head1 Summary

Makes the plots for the Winzler, Baker, and Carucci gametocyte data.
There are two plots and a legend.  Each plot is a time course.  There
are three sets of data.

=over

=item Percent Expression

This part is called C<pct> and contains the percentile data.

=item Absolute Expression

This part is called C<abs> and contains the absolute expression data.

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

   $Self->setPctQuery_3d7         ( $Args->{PctQuery_3d7        } );
   $Self->setPctQuery_Macs3d7     ( $Args->{PctQuery_Macs3d7    } );
   $Self->setPctQuery_NF54        ( $Args->{PctQuery_NF54       } );

   $Self->setAbsQuery_3d7         ( $Args->{AbsQuery_3d7        } );
   $Self->setAbsQuery_Macs3d7     ( $Args->{AbsQuery_Macs3d7    } );
   $Self->setAbsQuery_NF54        ( $Args->{AbsQuery_NF54       } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getPctQuery_3d7         { $_[0]->{'PctQuery_3d7'      } }
sub setPctQuery_3d7         { $_[0]->{'PctQuery_3d7'      } = $_[1]; $_[0] }

sub getPctQuery_Macs3d7     { $_[0]->{'PctQuery_Macs3d7'  } }
sub setPctQuery_Macs3d7     { $_[0]->{'PctQuery_Macs3d7'  } = $_[1]; $_[0] }

sub getPctQuery_NF54        { $_[0]->{'PctQuery_NF54'     } }
sub setPctQuery_NF54        { $_[0]->{'PctQuery_NF54'     } = $_[1]; $_[0] }

sub getAbsQuery_3d7         { $_[0]->{'AbsQuery_3d7'      } }
sub setAbsQuery_3d7         { $_[0]->{'AbsQuery_3d7'      } = $_[1]; $_[0] }

sub getAbsQuery_Macs3d7     { $_[0]->{'AbsQuery_Macs3d7'  } }
sub setAbsQuery_Macs3d7     { $_[0]->{'AbsQuery_Macs3d7'  } = $_[1]; $_[0] }

sub getAbsQuery_NF54        { $_[0]->{'AbsQuery_NF54'     } }
sub setAbsQuery_NF54        { $_[0]->{'AbsQuery_NF54'     } = $_[1]; $_[0] }

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

   my $pct_3d7_f     = eval { $Self->getPctQuery_3d7()    ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $pct_Macs3d7_f = eval { $Self->getPctQuery_Macs3d7()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $pct_NF54_f    = eval { $Self->getPctQuery_NF54()   ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   my $abs_3d7_f     = eval { $Self->getAbsQuery_3d7()    ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $abs_Macs3d7_f = eval { $Self->getAbsQuery_Macs3d7()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $abs_NF54_f    = eval { $Self->getAbsQuery_NF54()   ->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'LEGEND', Size => 40 },
                   { Name => 'pct',    Size => 200 },
                   { Name => 'abs',    Size => 200 },
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

data.pct.3d7       <- read.table("$pct_3d7_f",     header=T);
data.pct.macs3d7   <- read.table("$pct_Macs3d7_f", header=T);
data.pct.nf54      <- read.table("$pct_NF54_f",    header=T);

data.abs.3d7       <- read.table("$abs_3d7_f",     header=T);
data.abs.macs3d7   <- read.table("$abs_Macs3d7_f", header=T);
data.abs.nf54      <- read.table("$abs_NF54_f",    header=T);

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

x.min <- min(data.pct.3d7\$ELEMENT_ORDER, data.pct.macs3d7\$ELEMENT_ORDER, data.pct.nf54\$ELEMENT_ORDER,
             data.abs.3d7\$ELEMENT_ORDER, data.abs.macs3d7\$ELEMENT_ORDER, data.abs.nf54\$ELEMENT_ORDER
            );
x.max <- max(data.pct.3d7\$ELEMENT_ORDER, data.pct.macs3d7\$ELEMENT_ORDER, data.pct.nf54\$ELEMENT_ORDER,
             data.abs.3d7\$ELEMENT_ORDER, data.abs.macs3d7\$ELEMENT_ORDER, data.abs.nf54\$ELEMENT_ORDER
            );

# ----------------------- SCREEN : Legend --------------------------------

if ($isVis_b{LEGEND} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs="i", xaxs="i", xaxt="n", yaxt="n", bty="n", mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type="l",col="orange", xlim=c(0,1),ylim=c(0,1));

  legend(0.5, 0.5,
       c("3D7", "MACS-purified 3D7", "isolate NF54"),
       xjust = 0.5,
       yjust = 0.5,
       cex   = 0.9,
       pt.cex = 1.5,
       bty   = "n",
       col   = c("red", "pink", "purple"),
       pt.bg = c("red", "pink", "purple"),
       pch   = c(19,      19,      19),
       lty   = c("solid", "solid", "solid"),
       ncol  = 3,
       #horiz = T
      );
}

# ----------------------- SCREEN 2 : Pct Data Plot -----------------------

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  plasmodb.par.last();
  plot(data.pct.3d7\$ELEMENT_ORDER,
       data.pct.3d7\$VALUE,
       col  = "red",
       bg   = "red",
       type = "b",
       pch  = 19,
       cex  = 1.5,
       xlab = "Day of gametocytogenesis",
       xlim = c(x.min, x.max),
       ylab = "Expression Percentile",
       ylim = c(0,100)
      );
  lines( data.pct.macs3d7\$ELEMENT_ORDER,
       data.pct.macs3d7\$VALUE,
       col  = "pink",
       bg   = "pink",
       type = "b",
       pch  = 19,
       cex  = 1.5
       );
  lines(data.pct.nf54\$ELEMENT_ORDER,
       data.pct.nf54\$VALUE,
       col  = "purple",
       bg   = "purple",
       type = "o",
       pch  = 19,
       cex  = 1.5
       );
  plasmodb.ticks(1, 0, nrow(data.pct.nf54), 5);
  plasmodb.grid();
  plasmodb.title("Expression Levels (percentiled)");
}

# ----------------------- SCREEN 3 : Abs Data Plot -----------------------

if ($isVis_b{abs} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  ylim.min = min(-2, data.abs.3d7\$VALUE, data.abs.macs3d7\$VALUE, data.abs.nf54\$VALUE);
  ylim.max = max( 2, data.abs.3d7\$VALUE, data.abs.macs3d7\$VALUE, data.abs.nf54\$VALUE);

  plasmodb.par.last();
  plot(data.abs.3d7\$ELEMENT_ORDER,
       data.abs.3d7\$VALUE,
       col  = "red",
       bg   = "gray",
       type = "b",
       pch  = 19,
       cex  = 1.5,
       xlab = "Day of gametocytogenesis",
       xlim = c(x.min, x.max),
       ylab = "Absolute Expression",
       ylim = c(ylim.min, ylim.max)
      );
  lines( data.abs.macs3d7\$ELEMENT_ORDER,
       data.abs.macs3d7\$VALUE,
       col  = "pink",
       bg   = "gray",
       type = "b",
       pch  = 19,
       cex  = 1.5
       );
  lines(data.abs.nf54\$ELEMENT_ORDER,
       data.abs.nf54\$VALUE,
       col  = "purple",
       bg   = "gray",
       type = "o",
       pch  = 19,
       cex  = 1.5
       );
  plasmodb.ticks(1, 0, nrow(data.abs.nf54), 5);
  plasmodb.grid();
  plasmodb.title("Expression Levels (absolute)");
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f,
        $pct_3d7_f,   $pct_Macs3d7_f,
        $pct_NF54_f,   $abs_3d7_f,
        $abs_Macs3d7_f,   $abs_NF54_f
       );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
