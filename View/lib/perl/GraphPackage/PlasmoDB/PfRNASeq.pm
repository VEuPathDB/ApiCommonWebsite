package PlasmoDBWebsite::View::GraphPackage::PfRNASeq;

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
   $names = '"0h", "8h","16h","24h","32h","40h","48h"';

   my $names_n   = scalar @_names;

   my $r_f  = $Self->getOutputFile(). '.R'; 
   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors;

   my $biorep01Exp_f = eval { $Self->getBioRep01ExpressionQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else { 
     my $parts =  [ { Name => 'LEGEND', Size => 40  },
                    { Name => 'rat',    Size => 240 },
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

color.rep1      <- rgb(153,   0, 153, max=255);
colors.track1   <- c(color.rep1);

# ------------------------------ Load Data -------------------------------

data.names      <- c($names);

data.track1.table    <- read.table("$biorep01Exp_f", header=T);

data.track1.rat <- c(data.track1.table\$VALUE);

# ------------------------ Merge Data and Colors -------------------------

ylim.rat <- c(max(-10, min(0,data.track1.rat)),
                       min( 10, max( 2,data.track1.rat ))
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
         c("log2 of the geometric mean of coverage / kb of unique sequence"),
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.90,
         bty   = "n",
         lty   = "solid",
         col   = c(color.rep1),
         pt.bg = c(color.rep1),
         pch   = 22,
         horiz = T
        );
}


# ----------------- SCREEN : Induction and Repression ------------------

# plot points for both data sets, draw line at 0-induction, draw
# connecting lines, add X-axis line.

if ($isVis_b{rat} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  plot(data.track1.rat,
       ylim      = ylim.rat,
       pch       = 22,
       cex       = 1.25,
       col       = colors.track1,
       bg        = colors.track1,
       xlab      = "Life Stage",
       ylab      = "log2 of geometric mean of coverage",
       xaxt      = "n",
       yaxt      = "n"
      );

  lines (x = seq(1,9),
         y = data.track1.rat[seq(1,9)],
         type    = "c",
         col     = "gray80"
        );

  yAxis = axis(4,tick=F);
  yaxis.labels = vector();
  for(i in 1:length(yAxis)) {
    value = yAxis[i];
    yaxis.labels[i] = value;
  } 

  axis(2,at=yAxis,labels=yaxis.labels,tick=T);  

  axis(1,at=seq(1,7),labels=data.names,tick=T);
  plasmodb.grid(nx=NA,ny=NULL);
  lines (c(-100,100), c(0,0), col="gray25");
  plasmodb.title("Intraerythrocytic Cycle");
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
       );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
