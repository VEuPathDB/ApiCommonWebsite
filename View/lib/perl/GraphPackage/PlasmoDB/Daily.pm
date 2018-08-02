package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Daily;

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage );

use EbrcWebsiteCommon::View::GraphPackage;
use EbrcWebsiteCommon::View::MultiScreen;
use EbrcWebsiteCommon::Model::CannedQuery::Profile;
use Time::HiRes qw ( time );

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

# ------------------------------ Accessors -------------------------------

sub getDataNamesQuery       { $_[0]->{'DataNamesQuery'       } }
sub setDataNamesQuery       { $_[0]->{'DataNamesQuery'       } = $_[1]; $_[0] }

sub getDataQuery            { $_[0]->{'DataQuery'            } }
sub setDataQuery            { $_[0]->{'DataQuery'            } = $_[1]; $_[0] }

sub getDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } }
sub setDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } = $_[1]; $_[0] }

sub getPercentileYaxisLabel { $_[0]->{'PctYaxisLabel'        } }
sub setPercentileYaxisLabel { $_[0]->{'PctYaxisLabel'        } = $_[1]; $_[0] }

sub getPercentileNamesQuery { $_[0]->{'PctNamesQuery'        } }
sub setPercentileNamesQuery { $_[0]->{'PctNamesQuery'        } = $_[1]; $_[0] }

sub getPercentileQuery      { $_[0]->{'PctQuery'             } }
sub setPercentileQuery      { $_[0]->{'PctQuery'             } = $_[1]; $_[0] }

sub getPctIsDecimal         { $_[0]->{'PctIsDecimal'         } }
sub setPctIsDecimal         { $_[0]->{'PctIsDecimal'         } = $_[1]; $_[0] }

sub getDataColors           { $_[0]->{'DataColors'           } }
sub setDataColors           { $_[0]->{'DataColors'           } = $_[1]; $_[0] }

sub getPercentileColors     { $_[0]->{'PctColors'            } }
sub setPercentileColors     { $_[0]->{'PctColors'            } = $_[1]; $_[0] }

sub getCorrelationColors    { $_[0]->{'CorColors'            } }
sub setCorrelationColors    { $_[0]->{'CorColors'            } = $_[1]; $_[0] }

sub getTagRx                { $_[0]->{'TagRx'                } }
sub setTagRx                { $_[0]->{'TagRx'                } = $_[1]; $_[0] }

sub getCorrelationYaxisLabel { $_[0]->{'CorYaxisLabel'       } }
sub setCorrelationYaxisLabel { $_[0]->{'CorYaxisLabel'       } = $_[1]; $_[0] }

sub getCorrelationNamesQuery { $_[0]->{'CorNamesQuery'       } }
sub setCorrelationNamesQuery { $_[0]->{'CorNamesQuery'       } = $_[1]; $_[0] }

sub getCorrelationQuery      { $_[0]->{'CorQuery'            } }
sub setCorrelationQuery      { $_[0]->{'CorQuery'            } = $_[1]; $_[0] }

sub getClusterColors         { $_[0]->{'ClusterColors'       } }
sub setClusterColors         { $_[0]->{'ClusterColors'       } = $_[1]; $_[0] }

sub getClusterSampleMap      { $_[0]->{'ClusterSampleMap'    } }
sub setClusterSampleMap      { $_[0]->{'ClusterSampleMap'    } = $_[1]; $_[0] }

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

   my $_pct_names = eval { $Self->getPercentileNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_pct_data  = eval { $Self->getPercentileQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my @pctTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_pct_names;
      my @pctAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_pct_data;


      if($Self->getPctIsDecimal()) {
        @pctAvg  = map {$_ * 100 } @pctAvg;
      }

      @pctTags = &formatNames(@pctTags);



      my $pctTags  = join(', ', map { "'$_'" } @pctTags);
      my $pctAvg    = join(', ', @pctAvg);
      my $pctYlab   = $Self->getPercentileYaxisLabel();

      my $pctColorsArrayRef = $Self->getPercentileColors();
      my $defaultPctColor = $pctColorsArrayRef->[0];

      my $clusterMap = $Self->getClusterSampleMap();
      my $clusterColors = $Self->getClusterColors();

      my @correlationColors;
      foreach(@pctTags) {
        my $patientNumber;
        if( $Self->getTypeArg() eq 'patient-number' || $_ !~ /-/) {
          $patientNumber = $_;
        }
        else {
          ($patientNumber) = $_ =~ / - (\d*\.?\d*)$/;
        }
        my $cluster = $clusterMap->{$patientNumber};

        my $color = $clusterColors->{$cluster};
        $color = $defaultPctColor unless($color);

        push(@correlationColors, $color);
      }


      my $pctColors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @correlationColors);


      my $_mS = EbrcWebsiteCommon::View::MultiScreen->new
      ( Parts => [ #{ Name => 'hist',   Size => 275 },
                   { Name => 'pct',   Size => 200 },
                   #{ Name => 'cor',   Size => 200 },
                 ],
        VisibleParts => $Self->getVisibleParts(),
        Thumbnail    => $thumb_b
      );

      # used in R code to branch
      my %isVis_b     = $_mS->partIsVisible();
      $isVis_b{hist} = 0;
      $isVis_b{cor} = 0;

      my $width       = 800;
      my $totalHeight = $_mS->totalHeight();
      if ($thumb_b) {
         $width       *= 0.75;
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

pct.avg         <- c($pctAvg);
pct.tags        <- c($pctTags);
pct.colors      <- c($pctColors);

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

# -------------------- SCREEN 3 : Percentile Histogram Data Plot --------------------

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,1));

  c <- barplot(pct.avg,
               col       = pct.colors,
               ylab      = '$pctYlab',
               ylim      = c(0, 100),
                names.arg = pct.tags,
               las       = 2
              );

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(c, pct.avg - 0, c, pct.avg+0,
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

   push(@Rv, $r_f, $out_f );

   return @Rv;
}


sub formatNames {

  my @rv;

  foreach(@_) {
    my ($val, $patient) = split(':', $_);

    if($patient && $patient ne $val) {
      if($val =~ /^\d+\.\d+$/) {
        $val = sprintf("%.2f", $val);
      }

      push @rv, "$val - $patient";
    }
    else {
      push @rv, $val;
    }
  }
  return @rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

