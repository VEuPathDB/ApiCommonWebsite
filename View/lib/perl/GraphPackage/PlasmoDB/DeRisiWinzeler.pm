
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler;
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
	 $Self->setWinzeler_Sorb_Rat    ( $Args->{Winzeler_Sorb_Rat   } );
	 $Self->setWinzeler_Sorb_Pct    ( $Args->{Winzeler_Sorb_Pct   } );
	 $Self->setWinzeler_Temp_Rat    ( $Args->{Winzeler_Temp_Rat   } );
	 $Self->setWinzeler_Temp_Pct    ( $Args->{Winzeler_Temp_Pct   } );

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

sub getWinzeler_Sorb_Rat    { $_[0]->{'Winzeler_Sorb_Rat' } }
sub setWinzeler_Sorb_Rat    { $_[0]->{'Winzeler_Sorb_Rat' } = $_[1]; $_[0] }

sub getWinzeler_Sorb_Pct    { $_[0]->{'Winzeler_Sorb_Pct' } }
sub setWinzeler_Sorb_Pct    { $_[0]->{'Winzeler_Sorb_Pct' } = $_[1]; $_[0] }

sub getWinzeler_Temp_Rat    { $_[0]->{'Winzeler_Temp_Rat' } }
sub setWinzeler_Temp_Rat    { $_[0]->{'Winzeler_Temp_Rat' } = $_[1]; $_[0] }

sub getWinzeler_Temp_Pct    { $_[0]->{'Winzeler_Temp_Pct' } }
sub setWinzeler_Temp_Pct    { $_[0]->{'Winzeler_Temp_Pct' } = $_[1]; $_[0] }

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

   my $wsr_f = eval { $Self->getWinzeler_Sorb_Rat    ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $wsp_f = eval { $Self->getWinzeler_Sorb_Pct    ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $wtr_f = eval { $Self->getWinzeler_Temp_Rat    ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $wtp_f = eval { $Self->getWinzeler_Temp_Pct    ()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);

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
      ( Parts => [ { Name => 'LEGEND',   Size => 40  },
                   { Name => 'derisi',   Size => 175 },
                   { Name => 'winzeler', Size => 150 },
                   { Name => 'both',     Size => 150 },
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

      #my $preamble_R = $Self->_rPreamble();
      #my $open_R    = $Self->rOpenFile(480,480);

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

data.wsr <- read.table("$wsr_f", header=T);
data.wsp <- read.table("$wsp_f", header=T);
data.wtr <- read.table("$wtr_f", header=T);
data.wtp <- read.table("$wtp_f", header=T);

# --------------------------- Prepare To Plot ----------------------------

$open_R;

plasmodb.par();

color.temp       <- rgb(153,   0, 153, max=255);
color.sorb       <- rgb(  0, 153, 153, max=255);
color.spor       <- "black";
color.unreliable <- "gray60";

x.min = min( data.dhr\$ELEMENT_ORDER, data.d3r\$ELEMENT_ORDER, data.ddr\$ELEMENT_ORDER,
             data.wsr\$ELEMENT_ORDER, data.wtr\$ELEMENT_ORDER
           );
x.max = max( data.dhr\$ELEMENT_ORDER, data.d3r\$ELEMENT_ORDER, data.ddr\$ELEMENT_ORDER,
             data.wsr\$ELEMENT_ORDER, data.wtr\$ELEMENT_ORDER
           );

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

flooredLg   <- function(x) { if (!is.na(x) && x > 0) log(x, base=2) else NA };

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
       c("HB3", "3D7", "DD2", " ", "3D7 sorbitol", "3D7 temperature"),
       xjust = 0.5,
       yjust = 0.5,
       cex   = 0.90,
       bty   = "n", 
       col   = c("blue",  "red",   "orange", "white", color.sorb, color.temp),
       pt.bg = c("blue",  "red",   "orange", "white", color.sorb, color.temp),
       pch   = c(22,      22,      22,       22,      22,         22),
       lty   = c("solid", "solid", "solid",  "solid", "solid",    "solid"),
       ncol  = 3,
       #horiz = T
      );
}


# ----------------------- SCREEN 1 : DeRisi Ratios -----------------------

if ($isVis_b{derisi} == 1) {
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

# ---------------------- SCREEN : Winzeler Ratios ----------------------

if ($isVis_b{winzeler} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  # mean of non-zero components
  data.wsr.mean <- mean(data.wsr\$VALUE[data.wsr\$VALUE > 0]);
  print('a');
  data.wtr.mean <- mean(data.wtr\$VALUE[data.wtr\$VALUE > 0]);
  print('b');

  # take log relative to mean
  data.wsr.rat  <- unlist(lapply(data.wsr\$VALUE / data.wsr.mean, flooredLg));
  print('c');
  data.wtr.rat  <- unlist(lapply(data.wtr\$VALUE / data.wtr.mean, flooredLg));
  print('d');

  print(data.wsr.rat);
  print(data.wtr.rat);

  # get the range
  y.max <- min( 10, max( 2, data.wsr.rat, data.wtr.rat));
  if (is.na(y.max)) y.max <- 10;
  print('e');
  y.min <- max(-10, min(-2, data.wsr.rat, data.wtr.rat));
  if (is.na(y.min)) y.min <- -10;
  print('f');

  # plot

  plot(data.wsr\$ELEMENT_ORDER,
       data.wsr.rat,
       col  = color.sorb,
       bg   = color.sorb,
       type = "o",
       pch  = 22,
       lwd  = 3,
       xlab = "",
       xlim = c(x.min, x.max),
       ylab = "lg(Exp/Avg)",
       ylim = c(y.min, y.max)
      );
  lines(data.wtr\$ELEMENT_ORDER,
        data.wtr.rat,
        col  = color.temp,
        bg   = color.temp,
        type = "o",
        pch  = 22,
        lwd  = 3
      );
  plasmodb.grid();
  plasmodb.ticks(1,x.min,x.max,5);
  plasmodb.title("Winzeler - log ratios");
}

# ----------------------- SCREEN 3 : All Percents ------------------------

if ($isVis_b{both} == 1) {
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
  lines(data.wsp\$ELEMENT_ORDER, 100*data.wsp\$VALUE, col=color.sorb, lwd=3);
  lines(data.wtp\$ELEMENT_ORDER, 100*data.wtp\$VALUE, col=color.temp, lwd=3);

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
   $ddp_f, $wsr_f, $wsp_f, $wtr_f, $wtp_f );

   return @Rv;
}

#,=======================================================================
# ---------------------------- End of Package ----------------------------
#,=======================================================================

1;

=pod

=head1 History

Fri Dec  2 11:47:55 EST 2005 : Jonathan Schug

  Added protection against infinite values due to zeros in Winzeler
  data.

=cut
