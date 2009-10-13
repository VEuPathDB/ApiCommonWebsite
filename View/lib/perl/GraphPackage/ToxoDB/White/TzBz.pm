package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::View::MultiScreen;
use ApiCommonWebsite::Model::CannedQuery::Profile;

use Time::HiRes qw ( time );

sub init {
   my $Self = shift;
   my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

   return $Self;
}

sub getNamesQuery           { $_[0]->{'NamesQuery'           } }
sub setNamesQuery           { $_[0]->{'NamesQuery'           } = $_[1]; $_[0] }

sub getDataQuery            { $_[0]->{'DataQuery'            } }
sub setDataQuery            { $_[0]->{'DataQuery'            } = $_[1]; $_[0] }

sub getYaxisLabel           { $_[0]->{'YaxisLabel'       } }
sub setYaxisLabel           { $_[0]->{'YaxisLabel'       } = $_[1]; $_[0] }

sub getColors               { $_[0]->{'Colors'               } }
sub setColors               { $_[0]->{'Colors'               } = $_[1]; $_[0] }

sub getTagRx                { $_[0]->{'TagRx'                } }
sub setTagRx                { $_[0]->{'TagRx'                } = $_[1]; $_[0] }

sub makeR {
   my $Self = shift;

   my @Rv;

   my $_qh   = $Self->getQueryHandle();
   my $_dict = {};

   my $thumb_b   = $Self->getThumbnail();

   my $fmt       = $Self->getFormat();
   my $r_f       = $Self->getOutputFile(). '.R';
   my $out_f     = $Self->getOutputFile();

   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors = ();

   my $_names = eval { $Self->getNamesQuery()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);
   my $_data  = eval { $Self->getDataQuery ()->getValues($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my (@tags, @std, @avg);


      @tags = map { $_->{NAME}  } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_names;
      @avg  = map { $_->{VALUE} } sort { $a->{ELEMENT_ORDER} <=> $b->{ELEMENT_ORDER} } @$_data;
      @std  = (0) x scalar @tags;

      my $tags_n = scalar @tags;

      my $tags   = join(', ', map { "'$_'" } @tags);
      my $avg    = join(', ', @avg);
      my $ylab   = $Self->getYaxisLabel();

      my $std    = join(', ', @std);
      my $colors = join(', ', map { $_ =~ /\(/ ? $_ : "'$_'" } @{$Self->getColors()});

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
        ( Parts => [ { Name => 'hist',   Size => 250 },
#                     { Name => 'pct',   Size => 200 },
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

#the.tags        <- c($tags);
the.tags = c("Tachyzoite", "Compound 1", "pH=8.2");

the.avg.v         <- c($avg);
the.avg.m = rbind(the.avg.v[1:3], the.avg.v[4:6], the.avg.v[7:9]);

the.std         <- c($std);
the.colors      <- c($colors);

the.legend      <- c("GT1", "ME49", "CTGara");

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

# -------------------- SCREEN 2 : Percentile Histogram Data Plot --------------------

if ($isVis_b{hist} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  d.max = max(1.1 * the.avg.v, 0);

  d.max.default = 10;

  if(d.max < d.max.default) {
    d.max = d.max.default;
  }



  par(mar       = c(8,4,1,10), xpd=TRUE);

  c <- barplot(the.avg.m,
               col       = the.colors,
               ylab      = '$ylab',
               ylim      = c(0, d.max),
               beside    = TRUE,
                names.arg = the.tags,
               las = 2
              );

  legend(13, d.max, legend=the.legend, cex=0.9, fill=the.colors, inset=0.2) ;

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


1;

