package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold;

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

sub getShortNamesQuery      { $_[0]->{'ShortNamesQuery'           } }
sub setShortNamesQuery      { $_[0]->{'ShortNamesQuery'           } = $_[1]; $_[0] }

sub getPercentileQuery      { $_[0]->{'PercentileQuery'           } }
sub setPercentileQuery      { $_[0]->{'PercentileQuery'           } = $_[1]; $_[0] }

sub getMoidValuesQuery      { $_[0]->{'MoidValuesQuery'           } }
sub setMoidValuesQuery      { $_[0]->{'MoidValuesQuery'           } = $_[1]; $_[0] }

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

   my $maxLgRat  = 65000;
   my $minLgRat  = 150;

   my @_names    = $Self->getShortNamesQuery()->getValues($_qh, $_dict); 
   my $names     = join(',', map { '"'. $_->{NAME}. '"' } @_names);

   my $names_n   = scalar @_names;

   my $r_f  = $Self->getOutputFile(). '.R';
   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors;

   my $moid_f  = eval { $Self->getMoidValuesQuery(   )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $percentile_f  = eval { $Self->getPercentileQuery( )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ 
                   { Name => 'LEGEND', Size => 20 },
                   { Name => 'rat',    Size => 280 },
                   { Name => 'pct',    Size => 220 },
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


color.mild      <- 'lightskyblue2';
color.severity  <- 'red2';

my.colors = c(as.vector(matrix("lightskyblue2", ncol=1, nrow=8)),
              as.vector(matrix("red1", ncol=1, nrow=9))); 

# ------------------------------ Load Data -------------------------------

data.names     <- c($names);

data.moid.df    <- read.table("$moid_f",  header=T);
data.pct.df    <- read.table("$percentile_f",  header=T);

data.moid = data.moid.df\$VALUE;
data.pct = data.pct.df\$VALUE;


# ----------------------------- Data Limits ------------------------------

ylim.lgr      <- c(0, min($maxLgRat, min($minLgRat,max(data.moid)+2)));

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
         c("mild disease", "severe disease"),
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.99,
         bty   = "n",
         lty   = "solid",
         col   = c(color.mild, color.severity),
         pt.bg = c(color.mild, color.severity), 
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

  par(mar       = c(8,4,4,1), xpd=TRUE);

  barplot(data.moid,
          beside    = 1,
          las = 2,
          space     = c(0.1, 1),
          ylab      = "log2 of RMA normalized",
          names.arg = c($names),
          col       = my.colors,
          ylim      = ylim.lgr,
          axis.lty  = "solid"
         );

  #axis(1,at=seq(1,4),labels=data.names,tick=T);
#  plasmodb.grid(nx=NA,ny=NULL);
  #lines (c(-100,100), c(0,0), col="gray25");
  plasmodb.title("Ex Vivo Intraerythrocitic Expression Assays");
}

# ---------------------- SCREEN : Percentage Plot ----------------------

# do bar plot, try to get x-axis line.

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1), xpd=TRUE);

  barplot(data.pct,
          beside    = 1,
          space     = c(0.1, 1),
          ylab      = "percentiled",
          las       = 2,
          names.arg = data.names,
          col       = my.colors,
          ylim      = c(0,1),
          axis.lty  = "solid"
         );
  plasmodb.title("");
}

# --------------------------------- Done ---------------------------------

# close the graphics file and quit.

dev.off();

quit(save="no")

R
   }

   $r_fh->close();

   push(@Rv, $r_f, $moid_f, $percentile_f);

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
