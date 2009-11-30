
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiOverlay;
@ISA = qw( ApiCommonWebsite::View::GraphPackage );

=pod

=head1 Summary

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::View::MultiScreen;
use ApiCommonWebsite::Model::CannedQuery::Profile;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;
	 my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

	 $Self->setDeRisi_Hb3_Rat       ( $Args->{DeRisi_Hb3_Rat      } );
	 $Self->setDeRisi_Hb3_Pct       ( $Args->{DeRisi_Hb3_Pct      } );
	 $Self->setDeRisi_3d7_Rat       ( $Args->{DeRisi_3d7_Rat      } );
	 $Self->setDeRisi_3d7_Pct       ( $Args->{DeRisi_3d7_Pct      } );
	 $Self->setDeRisi_Dd2_Rat       ( $Args->{DeRisi_Dd2_Rat      } );
	 $Self->setDeRisi_Dd2_Pct       ( $Args->{DeRisi_Dd2_Pct      } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getDeRisi_Hb3_Rat       { $_[0]->{'DeRisi_Hb3_Rat'    } }
sub setDeRisi_Hb3_Rat       { $_[0]->{'DeRisi_Hb3_Rat'    } = $_[1]; $_[0] }

sub getDeRisi_Hb3_Pct       { $_[0]->{'DeRisi_Hb3_Pct'    } }
sub setDeRisi_Hb3_Pct       { $_[0]->{'DeRisi_Hb3_Pct'    } = $_[1]; $_[0] }

sub getDeRisi_3d7_Rat       { $_[0]->{'DeRisi_3d7_Rat'    } }
sub setDeRisi_3d7_Rat       { $_[0]->{'DeRisi_3d7_Rat'    } = $_[1]; $_[0] }

sub getDeRisi_3d7_Pct       { $_[0]->{'DeRisi_3d7_Pct'    } }
sub setDeRisi_3d7_Pct       { $_[0]->{'DeRisi_3d7_Pct'    } = $_[1]; $_[0] }

sub getDeRisi_Dd2_Rat       { $_[0]->{'DeRisi_Dd2_Rat'    } }
sub setDeRisi_Dd2_Rat       { $_[0]->{'DeRisi_Dd2_Rat'    } = $_[1]; $_[0] }

sub getDeRisi_Dd2_Pct       { $_[0]->{'DeRisi_Dd2_Pct'    } }
sub setDeRisi_Dd2_Pct       { $_[0]->{'DeRisi_Dd2_Pct'    } = $_[1]; $_[0] }

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

   my @errors;

   my $dhr_f = eval { $Self->getDeRisi_Hb3_Rat       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $dhp_f = eval { $Self->getDeRisi_Hb3_Pct       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $d3r_f = eval { $Self->getDeRisi_3d7_Rat       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $d3p_f = eval { $Self->getDeRisi_3d7_Pct       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $ddr_f = eval { $Self->getDeRisi_Dd2_Rat       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $ddp_f = eval { $Self->getDeRisi_Dd2_Pct       ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'LEGEND', Size => 40  },
                   { Name => 'lgr',    Size => 175 },
                   { Name => 'pct',    Size => 175 },
                 ],
        VisibleParts => $Self->getVisibleParts(),
        Thumbnail    => $thumb_b
      );

      # used in R code to branch
      my %isVis_b = $_mS->partIsVisible();

      my $width = 480;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width        = 250;
         $totalHeight *= 0.75;
      }

      # used in R code to set locations of screens
      my $screens     = $_mS->rScreenVectors();
      my $parts_n     = $_mS->numberOfVisibleParts();

      my $open_R    = $Self->rOpenFile($width, $totalHeight);
      my $preamble_R = $Self->_rStandardComponents($thumb_b);

      print $r_fh <<R;

# ------------------------------- Prepare --------------------------------

$preamble_R

# ---------------------------- Load Data Sets ----------------------------

data.dhr <- read.table("$dhr_f", header=T);
data.dhp <- read.table("$dhp_f", header=T);
data.d3r <- read.table("$d3r_f", header=T);
data.d3p <- read.table("$d3p_f", header=T);
data.ddr <- read.table("$ddr_f", header=T);
data.ddp <- read.table("$ddp_f", header=T);

# --------------------------- Prepare To Plot ----------------------------

$open_R;

plasmodb.par();

x.min = min( data.dhr\$ELEMENT_ORDER, data.d3r\$ELEMENT_ORDER, data.ddr\$ELEMENT_ORDER);
x.max = max( data.dhr\$ELEMENT_ORDER, data.d3r\$ELEMENT_ORDER, data.ddr\$ELEMENT_ORDER);

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

ticks <- function() {
  axis(1, at=seq(x.min, x.max, 1), labels=F, col="gray75");
  axis(1, at=seq(5*floor(x.min/5+0.5), x.max, 5), labels=F, col="gray50");
  axis(1);
}

# ----------------------- SCREEN : Legend --------------------------------

if ($isVis_b{LEGEND} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs="i", xaxs="i", xaxt="n", yaxt="n", bty="n", mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type="l",col="orange", xlim=c(0,1),ylim=c(0,1));

  legend(0.5, 0.5,
       c("HB3", "3D7", "DD2"),
       xjust = 0.5,
       yjust = 0.5,
       cex   = 0.90,
       bty   = "n",
       col   = c("blue",  "red",   "orange"),
       pt.bg = c("blue",  "red",   "orange"),
       pch   = c(22,      22,      22),
       lty   = c("solid", "solid", "solid"),
       ncol  = 3
       #horiz = T
      );
}


# ----------------------- SCREEN 1 : DeRisi Ratios -----------------------

if ($isVis_b{lgr} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  y.max = max( 2, data.dhr\$VALUE, data.d3r\$VALUE, data.ddr\$VALUE);
  y.min = min(-2, data.dhr\$VALUE, data.d3r\$VALUE, data.ddr\$VALUE);

  plot(data.dhr\$ELEMENT_ORDER,
       data.dhr\$VALUE,
       col  = "blue",
       bg   = "blue",
       type = "o",
       pch  = 22,
       xlab = "",
       xlim = c(x.min, x.max),
       ylab = "lg(Cy5/Cy3)",
       ylim = c(y.min, y.max)
      );
  lines(data.d3r\$ELEMENT_ORDER,
        data.d3r\$VALUE,
        col  = "red",
        bg   = "red",
        type = "o",
        pch  = 22
       );
  lines(data.ddr\$ELEMENT_ORDER,
        data.ddr\$VALUE,
        col  = "orange",
        bg   = "orange",
        type = "o",
        pch  = 22
      ) ;
  plasmodb.grid();
  plasmodb.ticks(1,x.min,x.max,5);
  plasmodb.title("DeRisi - log ratios");
}

# ----------------------- SCREEN 3 : All Percents ------------------------

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  plasmodb.par.last();
  plot (data.dhp\$ELEMENT_ORDER, 100*data.dhp\$VALUE, col="blue", lwd=3,
        type="l",
        xlab = "Hours Post-Erythtocytic Invasion",
        xlim = c(x.min, x.max),
        ylab = "%",
        ylim = c(0,100),
        lab  = c(5,1,1)
       );
  lines(data.d3p\$ELEMENT_ORDER, 100*data.d3p\$VALUE, col="red", lwd=3);
  lines(data.ddp\$ELEMENT_ORDER, 100*data.ddp\$VALUE, col="orange", lwd=3);

  plasmodb.grid();
  plasmodb.ticks(1,x.min,x.max,5);
  plasmodb.title("Combined - Expression (percentiles)");
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R
   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f, $dhr_f, $dhp_f, $d3r_f, $d3p_f, $ddr_f,
   $ddp_f );

   return @Rv;
}

#,=======================================================================
# ---------------------------- End of Package ----------------------------
#,=======================================================================

1;

=pod

=head1 History

Thursday, April 6, 2006 1:39:28 PM : Jonathan Schug

  Initial creation by copy-and-cut from DerisiWinzeler.pm

=cut
