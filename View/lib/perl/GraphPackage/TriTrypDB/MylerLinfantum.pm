package ApiCommonWebsite::View::GraphPackage::TriTrypDB::MylerLinfantum;

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

use Data::Dumper;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;
   my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

   return $Self;
}

# ========================================================================
# ----------------------- Symbolic Access to Data ------------------------
# ========================================================================


sub setExpressionNames            { $_[0]->{'ExpressionNames'           } = $_[1]; $_[0] }
sub getExpressionNames            { $_[0]->{'ExpressionNames'           } }

sub setBioRep01ExpressionQuery    { $_[0]->{'BioRep01ExpressionQuery'   } = $_[1]; $_[0] }
sub getBioRep01ExpressionQuery    { $_[0]->{'BioRep01ExpressionQuery'   } }

sub setBioRep02ExpressionQuery    { $_[0]->{'BioRep02ExpressionQuery'   } = $_[1]; $_[0] }
sub getBioRep02ExpressionQuery    { $_[0]->{'BioRep02ExpressionQuery'   } }

sub setPercentileNames            { $_[0]->{'PercentileNames'           } = $_[1]; $_[0] }
sub getPercentileNames            { $_[0]->{'PercentileNames'           } }

sub setBioRep01PercentileQuery    { $_[0]->{'BioRep01PercentileQuery'   } = $_[1]; $_[0] }
sub getBioRep01PercentileQuery    { $_[0]->{'BioRep01PercentileQuery'   } }

sub setBioRep02PercentileQuery    { $_[0]->{'BioRep02PercentileQuery'   } = $_[1]; $_[0] }
sub getBioRep02PercentileQuery    { $_[0]->{'BioRep02PercentileQuery'   } }

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

   my $thumb_b   = $Self->getThumbnail();

   my @_names    = $Self->getExpressionNames()->getValues($_qh, $_dict);

   my $names     = join(',', map { qq{ "$_->{NAME}"}} @_names);
   my $names_n   = scalar @_names;

   my $r_f  = $Self->getOutputFile(). '.R';
   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors;

   my $biorep01Exp_f = eval { $Self->getBioRep01ExpressionQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $biorep02Exp_f = eval { $Self->getBioRep02ExpressionQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   my $biorep01Per_f = eval { $Self->getBioRep01PercentileQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $biorep02Per_f = eval { $Self->getBioRep02PercentileQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {
     my $parts =  [ { Name => 'LEGEND', Size => 40  },
                    { Name => 'fc',    Size => 240 },
                  ];

     unless($thumb_b) {
       push @$parts, { Name => 'pct',    Size => 240 };
     }

     my $_mS = ApiCommonWebsite::View::MultiScreen->new
       ( Parts => $parts,
         VisibleParts  => $Self->getVisibleParts(),
         Thumbnail     => $thumb_b,
       );

      # used in R code to branch
      my %isVis_b = $_mS->partIsVisible();

      my $width = 480;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width        = 250;
         $totalHeight *= 0.4;
         $isVis_b{pct} = 0;
      }

      # used in R code to set locations of screens
      my $screens     = $_mS->rScreenVectors();
      my $parts_n     = $_mS->numberOfVisibleParts();

      my $open_R    = $Self->rOpenFile($width, $totalHeight);
      my $preamble_R = $Self->_rStandardComponents($thumb_b);

      print $r_fh <<R;

# ------------------------------ Libraries -------------------------------

$preamble_R

# ------------------------------ Constants -------------------------------

color.rep1       <- rgb(153,   0, 153, max=255);
color.rep2       <- rgb(  0, 153, 153, max=255);

colors.track1        <- c(color.rep1);
colors.track2        <- c(color.rep2);

pct.colors      <- c(color.rep1, color.rep2);

# ------------------------------ Load Data -------------------------------

data.names       <- c($names);

data.track1.table    <- read.table("$biorep01Exp_f", header=T);
data.track2.table    <- read.table("$biorep02Exp_f", header=T);

data.track1.rat <- c(data.track1.table\$VALUE);
data.track2.rat <- c(data.track2.table\$VALUE);

data.percents1.table    <- read.table("$biorep01Per_f", header=T);
data.percents2.table    <- read.table("$biorep02Per_f", header=T);

data.percents1 <- data.percents1.table\$VALUE * 100;
data.percents2 <- data.percents2.table\$VALUE * 100;

pct.all         <- rbind(data.percents1, data.percents2);

# ------------------------ Merge Data and Colors -------------------------

ylim.rat             <- c(max(-10, min(-2,min(data.track1.rat, data.track2.rat))),
                          min( 10, max( 2,max(data.track1.rat, data.track2.rat)))
                         );


# --------------------------- Prepare To Plot ----------------------------

# open output file; set general plotting parameters; get ready to make
# three subplots.

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

# ------------------ SCREEN : Legend -----------------------------------

if ($isVis_b{LEGEND} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs="i", xaxs="i", xaxt="n", yaxt="n", bty="n", mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type="l",col="orange", xlim=c(0,1),ylim=c(0,1));

  legend(0.5, 0.5,
         c("Replicate 1", "Replicate 2"),
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.90,
         bty   = "n",
         lty   = "solid",
         col   = c(color.rep1, color.rep2),
         pt.bg = c(color.rep1, color.rep2),
         pch   = 22,
         horiz = T
        );
}


# ----------------- SCREEN : Induction and Repression ------------------

# plot points for both data sets, draw line at 0-induction, draw
# connecting lines, add X-axis line.

if ($isVis_b{fc} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  plot(data.track1.rat,
       ylim      = ylim.rat,
       pch       = 22,
       cex       = 1.25,
       col       = colors.track1,
       bg        = colors.track1,
       xlab      = "Life Stage",
       ylab      = "Fold Induction (vs 0 hr)",
       xaxt      = "n",
       yaxt      = "n"
      );

  points(data.track2.rat,
         pch       = 22,
         cex       = 1.25,
         col       = colors.track2,
         bg        = colors.track2
       );

  lines (x = seq(1,8),
         y = data.track1.rat[seq(1,8)],
         type    = "c",
         col     = "gray80"
        );
  lines (seq(1,8),
         data.track2.rat[seq(1,8)],
         type    = "c",
         col     = "gray80"
        );

  yAxis = axis(4,tick=F);
  yaxis.labels = vector();
  for(i in 1:length(yAxis)) {
    value = yAxis[i];
    if(value > 0) {
      yaxis.labels[i] = 2^value;
    }
    if(value < 0) {
      yaxis.labels[i] = -1 * (1 / (2^value));
    }
    if(value == 0) {
      yaxis.labels[i] = 0;
    }
  }


  axis(2,at=yAxis,labels=yaxis.labels,tick=T);  

  axis(1,at=seq(1,8),labels=data.names,tick=T);
  plasmodb.grid(nx=NA,ny=NULL);
  lines (c(-100,100), c(0,0), col="gray25");
  plasmodb.title("Differentiation Time Series: Promastigote (0 hr) to Amastigote (144 hr)");
}

## ---------------------- SCREEN : Percentile Plot ----------------------

# do bar plot, try to get x-axis line.

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1), xpd=TRUE);

  barplot(pct.all,
          beside    = 1,
          space     = c(0.1, 1),
          ylab      = "%",
          names.arg = data.names,
          col       = pct.colors,
          border    = pct.colors,
          ylim      = c(0,100),
          axis.lty  = "solid",
          las = 2
         );

  plasmodb.title("Expression levels (percentiled)");
}


## --------------------------------- Done ---------------------------------

## close the graphics file and quit.

dev.off();

quit(save="no");

R
   }

   $r_fh->close();

   push(@Rv, $r_f,
        $biorep01Exp_f,
        $biorep02Exp_f
       );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
