
package PlasmoDBWebsite::View::GraphPackage::Cowman;

=pod

=head1 Summary

Makes the plots for the Cowman data.

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

   $Self->setInvasionDataQuery    ( $Args->{InvasionDataQuery   } );
   $Self->setInvasionNamesQuery   ( $Args->{InvasionNamesQuery  } );

   $Self->setSir2KoDataQuery      ( $Args->{Sir2KoDataQuery     } );
   $Self->setSir2KoNamesQuery     ( $Args->{Sir2KoNamesQuery    } );

   $Self->setPathwaysDataQuery    ( $Args->{PathwaysDataQuery   } );
   $Self->setPathwaysNamesQuery   ( $Args->{PathwaysNamesQuery  } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getInvasionDataQuery    { $_[0]->{'InvasionDataQuery' } }
sub setInvasionDataQuery    { $_[0]->{'InvasionDataQuery' } = $_[1]; $_[0] }

sub getInvasionNamesQuery   { $_[0]->{'InvasionNamesQuery'} }
sub setInvasionNamesQuery   { $_[0]->{'InvasionNamesQuery'} = $_[1]; $_[0] }

sub getSir2KoDataQuery      { $_[0]->{'Sir2KoDataQuery'   } }
sub setSir2KoDataQuery      { $_[0]->{'Sir2KoDataQuery'   } = $_[1]; $_[0] }

sub getSir2KoNamesQuery     { $_[0]->{'Sir2KoNamesQuery'  } }
sub setSir2KoNamesQuery     { $_[0]->{'Sir2KoNamesQuery'  } = $_[1]; $_[0] }

sub getPathwaysDataQuery    { $_[0]->{'PathwaysDataQuery' } }
sub setPathwaysDataQuery    { $_[0]->{'PathwaysDataQuery' } = $_[1]; $_[0] }

sub getPathwaysNamesQuery   { $_[0]->{'PathwaysNamesQuery'} }
sub setPathwaysNamesQuery   { $_[0]->{'PathwaysNamesQuery'} = $_[1]; $_[0] }

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

   my $inva_data_f = eval { $Self->getInvasionDataQuery ()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $s2ko_data_f = eval { $Self->getSir2KoDataQuery   ()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $path_data_f = eval { $Self->getPathwaysDataQuery ()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   my $inva_name_f = eval { $Self->getInvasionNamesQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $s2ko_name_f = eval { $Self->getSir2KoNamesQuery  ()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $path_name_f = eval { $Self->getPathwaysNamesQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   #my $invaKey     = 'Invasion by P. falciparum merozoites';
   #my $s2koKey     = 'P.falciparum strain 3D7, SIR2 knockout';
   #my $pathKey     = 'P.falciparum invasion pathways';

   # read these from bottom to top
   my $keys        = join(",\n",
                          map { "'$_'" }

                          # invasion
                          'the D10 strain)',
                          'ligand KO (Rh2b, EBA175, EBA140; Rh2b and EBA140 are deleted in',
                          'Effect during P. falciparum merozoite invasion of red cell receptor',

                          #
                          'Disruption of gene silencing by KO of the SIR2 gene in P. falciparum',

                          # pathway
                          '(W2mef/c4/Nm, W2mef EBA175 KO) red cell receptor invasion pathway',
                          'P. Falciparum sialic acid-dependent (W2mef) vs. -independent',
                         );

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'LEGEND', Size => 100 },
                   { Name => 'hist',   Size => 350 },
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

#merge <- function (i, s, p) { c( i, s, p) }

# REMEMBER: BAR GRAPH RUNS BOTTOM TO TOP

# data functions
mergeData <- function (i, s, p) {
  c( i[1],
     i[2],
     (i[3]+i[4])/2,
     (i[7]+i[8])/2,
     (i[9]+i[10])/2,

     i[5],
     i[6],
     (s[1]+s[2])/2,
     (s[3]+s[4])/2,

     p
   )
}

# error bar functions
eb         <- function (d)       { sqrt(var(d)) }

ebData     <- function (i, s, p) {
  c( 0,
     0,
     eb(c(i[3], i[4])),
     eb(c(i[7], i[8])),
     eb(c(i[9], i[10])),

     0,
     0,
     eb(c(s[1], s[2])),
     eb(c(s[3], s[4])),

     0,
     0,
     0
   )
}

# name functions
mergeName <- function (i, s, p) {
  c( '3D7 EBA140 KO 48hr rep1',
     '3D7 EBA175 KO 48hr rep1',
     '3D7 PfRh2b KO 48hr (rep1+rep2)/2',
     '3D7 WT 48hr (rep1+rep2)/2',
     'D10 WT 48hr (rep1+rep2)/2',

     '3D7 SIR2 KO 24hr rep1',
     '3D7 WT 24hr rep1',
     '3D7 SIR2 KO 8hr (rep1+rep2)/2',
     '3D7 WT 8hr (rep1+rep2)/2',

     'W2mef EBA175 KO (late T) rep1',
     'W2mef WT (late T) rep1',
     'W2mef/c4/Nm (late T) rep1'
   )
}

data.inva   <- read.table("$inva_data_f", header=T, sep="\t");
data.s2ko   <- read.table("$s2ko_data_f", header=T, sep="\t");
data.path   <- read.table("$path_data_f", header=T, sep="\t");

name.inva   <- read.table("$inva_name_f", header=T, sep="\t");
name.s2ko   <- read.table("$s2ko_name_f", header=T, sep="\t");
name.path   <- read.table("$path_name_f", header=T, sep="\t");

color.inva  <- 'red';
color.s2ko  <- 'green';
color.path  <- 'blue';

data <- mergeData(data.inva[,2], data.s2ko[,2], data.path[,2]);
ebs  <- ebData(data.inva[,2], data.s2ko[,2], data.path[,2]);
name <- mergeName(levels(name.inva[,2]), levels(name.s2ko[,2]), levels(name.path[,2]));

#colors <- merge( rep(color.inva,length(data.inva[,1])),
#                 rep(color.s2ko,length(data.s2ko[,1])),
#                 rep(color.path,length(data.path[,1]))
#               );

colors <- c( rep(color.inva,5),
             rep(color.s2ko,4),
             rep(color.path,length(data.path[,1]))
           );

keys <- c($keys);

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

# ----------------------- SCREEN : Legend --------------------------------

if ($isVis_b{LEGEND} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(yaxs="i", xaxs="i", xaxt="n", yaxt="n", bty="n", mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type="l",col="orange", xlim=c(0,1),ylim=c(0,1));

  # keys and colors are reversed since the horiz bar graph goes bottom to top.
  legend(0.5, 0.5,
         rev(keys),
         xjust  = 0.5,
         yjust  = 0.5,
         cex    = 1.1,
         bty    = "n",
         #lty   = "solid",
         #lw    = 5,
         col    = rev(c( 'white', 'white', color.inva, color.s2ko, 'white', color.path)),
         pch    = 15,
         pt.cex = 2,
         horiz  = F
        );
}

# -------------------- SCREEN 2 : Histogram Data Plot --------------------

if ($isVis_b{hist} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar=c(5,18,0,1), las=1);

  x.max <- max( 0, data + ebs );
  x.min <- min( 0, data - ebs );
  x.del <- (x.max - x.min) / 20;
  x.min <- x.min - x.del;
  x.max <- x.max + x.del;

  y <- barplot(data,
               names.arg = name,
               col       = colors,
               horiz     = T,
               #xlab      = 'log2(data)',
               xlab      = 'Expression Level
',
               xlim      = c(x.min, x.max)
              );

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(data-ebs, y, data+ebs, y,
                           col="black",
                           lw=2,
                           angle=90,
                           code=3,
                           length=0.05
                           )
                   );
  box();
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv,
        $r_f, $out_f,
        $inva_data_f, $s2ko_data_f, $path_data_f,
        $inva_name_f, $s2ko_name_f, $path_name_f,
       );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
