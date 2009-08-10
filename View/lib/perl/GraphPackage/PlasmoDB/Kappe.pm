package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kappe;

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

#   $Self->setNamesQuery           ( $Args->{NamesQuery} );
#   $Self->setDataQuery            ( $Args->{DataQuery } );
#   $Self->setYaxisLabel           ( $Args->{YaxisLabel          } );
#   $Self->setColors               ( $Args->{Colors              } );
#   $Self->setTagRx                ( $Args->{TagRx               } );


   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getDataNamesQuery       { $_[0]->{'DataNamesQuery'       } }
sub setDataNamesQuery       { $_[0]->{'DataNamesQuery'       } = $_[1]; $_[0] }

sub getDataQuery            { $_[0]->{'DataQuery'            } }
sub setDataQuery            { $_[0]->{'DataQuery'            } = $_[1]; $_[0] }

sub getDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } }
sub setDataYaxisLabel       { $_[0]->{'DataYaxisLabel'       } = $_[1]; $_[0] }

sub getNumNamesQuery        { $_[0]->{'NumNamesQuery'        } }
sub setNumNamesQuery        { $_[0]->{'NumNamesQuery'        } = $_[1]; $_[0] }

sub getNumQuery             { $_[0]->{'NumQuery'             } }
sub setNumQuery             { $_[0]->{'NumQuery'             } = $_[1]; $_[0] }

sub getPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } }
sub setPctYaxisLabel        { $_[0]->{'PctYaxisLabel'        } = $_[1]; $_[0] }

sub getDenNamesQuery        { $_[0]->{'DenNamesQuery'        } }
sub setDenNamesQuery        { $_[0]->{'DenNamesQuery'        } = $_[1]; $_[0] }

sub getDenQuery             { $_[0]->{'DenQuery'             } }
sub setDenQuery             { $_[0]->{'DenQuery'             } = $_[1]; $_[0] }

sub getPctIsDecimal         { $_[0]->{'PctIsDecimal'         } }
sub setPctIsDecimal         { $_[0]->{'PctIsDecimal'         } = $_[1]; $_[0] }

sub getColors               { $_[0]->{'Colors'            } }
sub setColors               { $_[0]->{'Colors'            } = $_[1]; $_[0] }

sub getTagRx                { $_[0]->{'TagRx'             } }
sub setTagRx                { $_[0]->{'TagRx'             } = $_[1]; $_[0] }

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

   my $_names = eval { $Self->getDataNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_data  = eval { $Self->getDataQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   my $_num_names = eval { $Self->getNumNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_num_data  = eval { $Self->getNumQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   my $_den_names = eval { $Self->getDenNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_den_data  = eval { $Self->getDenQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);



   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      # data we need to get for plot
      my (@tags, @nTags, @dTags);
      my (@avg, @nAvg, @dAvg);
      my @std;

      @tags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_names;
      @avg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_data;

      @nTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_num_names;
      @nAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_num_data;

      @dTags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_den_names;
      @dAvg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_den_data;

      my @pctTags;

      for(my $i = 0; $i < scalar @nTags; $i++) {
        push @pctTags, $nTags[$i];
        push @pctTags, $dTags[$i];
      }

      if($Self->getPctIsDecimal()) {
        @nAvg  = map {$_ * 100 } @nAvg;
        @dAvg  = map {$_ * 100 } @dAvg;
      }


      @std  = (0) x scalar @tags;

      my $tags_n = scalar @tags;

      my $tags   = join(', ', map { "'$_'" } @tags);
      my $avg    = join(', ', @avg);
      my $ylab   = $Self->getDataYaxisLabel();

      my $nAvg    = join(', ', @nAvg);
      my $nYlab   = $Self->getPctYaxisLabel();

      my $dAvg    = join(', ', @dAvg);
      my $dYlab   = $Self->getPctYaxisLabel();

      my $pctTags  = join(', ', map { "'$_'" } @pctTags);

      my $std    = join(', ', @std);
      my $colors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @{$Self->getColors()});

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'hist',   Size => 275 },
                   { Name => 'pct',   Size => 200 },
                 ],
        VisibleParts => $Self->getVisibleParts(),
        Thumbnail    => $thumb_b
      );

      # used in R code to branch
      my %isVis_b     = $_mS->partIsVisible();

      my $width       = 600;
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

the.tags        <- c($tags);
the.avg         <- c($avg);

n.avg           <- c($nAvg);
d.avg           <- c($dAvg);

pct.all         <- rbind(n.avg, d.avg);
pct.tags        <- c($pctTags);

the.std         <- c($std);
the.colors      <- c($colors);
pct.colors      <- c("darkred", "darkgreen");
pct.legend      <- c("Numerator", "Denominator");

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

# -------------------- SCREEN 2 : Histogram Data Plot --------------------

if ($isVis_b{hist} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,10));

  d.min = min(1.1 * (the.avg - the.std), 0);
  d.max = max(1.1 * (the.avg + the.std), 0);

  d.min.default = -1;
  d.max.default = 1;

  if(d.min > d.min.default) {
    d.min = d.min.default;
  }

  if(d.max < d.max.default) {
    d.max = d.max.default;
  }

  c <- barplot(the.avg,
               col       =the.colors,
               ylab      = '$ylab',
               ylim      = c(d.min, d.max),
              );

  text(c, par("usr")[3], labels = the.tags, srt = 45, adj = c(1,1), xpd = TRUE, cex=0.85);
  axis(2);

  # the suppressWarnings hides complaints about zero-length arrows
  suppressWarnings( arrows(c, the.avg - the.std, c, the.avg+the.std,
                           col="black",
                           lw=2,
                           angle=90,
                           code=3,
                           length=0.05
                           )
                   );
  box();
}

# -------------------- SCREEN 2 : Percentile Histogram Data Plot --------------------

if ($isVis_b{pct} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  par(mar       = c(8,4,1,10), xpd=TRUE);

  c <- barplot(pct.all,
               col       = pct.colors,
               ylab      = '$dYlab',
               ylim      = c(0, 100),
               beside    = TRUE,
                names.arg = pct.tags,
               las = 2
              );

  legend(33,0.75, pct.legend, fill=pct.colors) ;

  # the suppressWarnings hides complaints about zero-length arrows
  #suppressWarnings( arrows(c, pct.all - the.std, c, pct.all+the.std,
  #                         col="black",
  #                         lw=2,
  #                         angle=90,
  #                         code=3,
  #                         length=0.05
  #                         )
  #                 );





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

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

