package PlasmoDBWebsite::View::GraphPackage::WinzelerYoelii;

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

   my $minLgRat  = 100;
   my $maxLgRat  = 65000;

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
      ( Parts => [ { Name => 'rat',    Size => 240 },
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

# Steve, change colors here.
color.strain.hp  <- rgb(153,   0,   0, max=255);
color.strain.hpe <- rgb(  0,   0, 153, max=255);

my.colors = c(as.vector(matrix("darkgreen", ncol=1, nrow=2)),
              as.vector(matrix("DarkSeaGreen", ncol=1, nrow=2)),
              as.vector(matrix("khaki", ncol=1, nrow=1)),
              as.vector(matrix("orange", ncol=1, nrow=2)),
              as.vector(matrix("DarkGoldenRod", ncol=1, nrow=3)),
              as.vector(matrix("DarkGoldenRod3", ncol=1, nrow=2)),
              as.vector(matrix("DarkGoldenRod4", ncol=1, nrow=2)));

# ------------------------------ Load Data -------------------------------

data.names     <- c($names);

data.moid.df    <- read.table("$moid_f",  header=T);
data.pct.df    <- read.table("$percentile_f",  header=T);

data.moid = data.moid.df\$VALUE;
data.pct = data.pct.df\$VALUE;


# ----------------------------- Data Limits ------------------------------

ylim.lgr      <- c(0, min($maxLgRat, max($minLgRat, max(data.moid)+100)));

# --------------------------- Prepare To Plot ----------------------------

# open output file; set general plotting parameters; get ready to make
# three subplots.

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screen.i    <- 1;

# ----------------- SCREEN : Induction and Repression ------------------

# plot points for both data sets, draw line at 0-induction, draw
# connecting lines, add X-axis line.

if ($isVis_b{rat} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1), xpd=TRUE);

  barplot(data.moid,
          beside    = 1,
          las = 2,
          space     = c(0.1, 1),
          ylab      = "moid expression values",
          names.arg = c($names),
          col       = my.colors,
#          border    = c(color.strain.hp, color.strain.hpe),
          ylim      = ylim.lgr,
          axis.lty  = "solid"
         );

  #axis(1,at=seq(1,4),labels=data.names,tick=T);
#  plasmodb.grid(nx=NA,ny=NULL);
  #lines (c(-100,100), c(0,0), col="gray25");
  plasmodb.title("Profile of the P. yoelii life cycle - 14 samples");
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
          ylab      = "%",
          las       = 2,
          names.arg = data.names,
          col       = my.colors,
#          border    = c(color.strain.hp, color.strain.hpe),
          ylim      = c(0,100),
          axis.lty  = "solid"
         );
  plasmodb.title("Expression Levels (Percentiled)");
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
