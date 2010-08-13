
package ApiCommonWebsite::View::GraphPackage::SimilarityProfile;
@ISA = qw( ApiCommonWebsite::View::GraphPackage );

=pod

=head1 Summary

This package draws an expression profile that has been used as a query
to identify genes with a similar expression profile.

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

   $Self->setMatchProfile         ( $Args->{MatchProfile        } );
   $Self->setQueryProfile         ( $Args->{QueryProfile        } );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getMatchProfile         { $_[0]->{'MatchProfile'      } }
sub setMatchProfile         { $_[0]->{'MatchProfile'      } = $_[1]; $_[0] }

sub getQueryProfile         { $_[0]->{'QueryProfile'      } }
sub setQueryProfile         { $_[0]->{'QueryProfile'      } = $_[1]; $_[0] }

sub getYmin                 { $_[0]->{'YMin'              } }
sub setYmin                 { $_[0]->{'YMin'              } = $_[1]; $_[0] }

sub getYmax                 { $_[0]->{'YMax'              } }
sub setYmax                 { $_[0]->{'YMax'              } = $_[1]; $_[0] }

sub getSmoothSpline         { $_[0]->{'SmoothSpline'      } }
sub setSmoothSpline         { $_[0]->{'SmoothSpline'      } = $_[1]; $_[0] }

sub getSplineApproxN        { $_[0]->{'SplineApproxN'     } }
sub setSplineApproxN        { $_[0]->{'SplineApproxN'     } = $_[1]; $_[0] }


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

   my $match_f = eval { $Self->getMatchProfile()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);
   my $query_f = eval { $Self->getQueryProfile()->makeTabFile($_qh,$_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {

      my $_mS = ApiCommonWebsite::View::MultiScreen->new
      ( Parts => [ { Name => 'LEGEND', Size => 40  },
                   { Name => 'lgr',    Size => 125 },
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


      my $yMin = $Self->getYmin() ? $Self->getYmin() : -2;
      my $yMax = $Self->getYmax() ? $Self->getYmax() : 2;

      my $smoothLines = defined($Self->getSmoothSpline()) ? 'TRUE' : 'FALSE';
      my $splineApproxN = defined($Self->getSplineApproxN()) ? $Self->getSplineApproxN() : 60;

      print $r_fh <<R;

# ------------------------------- Prepare --------------------------------

$preamble_R

# ---------------------------- Load Data Sets ----------------------------

data.match <- read.table("$match_f", header=T);
data.query <- read.table("$query_f", header=T);

# --------------------------- Prepare To Plot ----------------------------

$open_R;

plasmodb.par();

x.min = min( data.match\$ELEMENT_ORDER, data.query\$ELEMENT_ORDER);
x.max = max( data.match\$ELEMENT_ORDER, data.query\$ELEMENT_ORDER);

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
       c("Match", "Query"),
       xjust = 0.5,
       yjust = 0.5,
       cex   = 1.5,
       bty   = "n",
       col   = c("blue",  "gray" ),
       pt.bg = c("blue",  "gray" ),
       pch   = c(22,      22     ),
       lty   = c("solid", "solid"),
       ncol  = 3,
       #horiz = T
      );
}


if ($isVis_b{lgr} == 1) {
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  y.max = max( $yMax, data.match\$VALUE, data.query\$VALUE);
  y.min = min( $yMin, data.match\$VALUE, data.query\$VALUE);

  if($smoothLines) {
    approxInterpMatch = approx(data.match\$ELEMENT_ORDER, n=$splineApproxN);
    predict_x_match = approxInterpMatch\$y;

    approxInterpQuery = approx(data.query\$ELEMENT_ORDER, n=$splineApproxN);
    predict_x_query = approxInterpQuery\$y;


    plot(predict(smooth.spline(x=data.match\$ELEMENT_ORDER, y=data.match\$VALUE),predict_x_query),
         col  = "blue",
         bg   = "blue",
         type = "l",
         xlab = "",
         xlim = c(x.min, x.max),
         ylab = "Expr Val",
         ylim = c(y.min, y.max),
         xaxt="n"
        );

    points(data.match\$ELEMENT_ORDER, y=data.match\$VALUE,
         col  = "blue",
         bg   = "blue",
         type = \"p\",
         pch  = 19,
         cex  = 1
         );



    lines(predict(smooth.spline(x=data.query\$ELEMENT_ORDER, y=data.query\$VALUE),predict_x_query),
          col  = "black",
          bg   = "black",
         );

    points(data.query\$ELEMENT_ORDER, y=data.query\$VALUE,
         col  = "black",
         bg   = "black",
         type = \"p\",
         pch  = 22,
         cex  = 1
         );


  } else {
    plot(data.match\$ELEMENT_ORDER,
         data.match\$VALUE,
         col  = "blue",
         bg   = "blue",
         type = "o",
         pch  = 22,
         xlab = "",
         xlim = c(x.min, x.max),
         ylab = "Expr Val",
         ylim = c(y.min, y.max)
        );
    lines(data.query\$ELEMENT_ORDER,
          data.query\$VALUE,
          col  = "gray",
          bg   = "gray",
          type = "o",
          pch  = 22
         );
  }

  plasmodb.grid();
  plasmodb.ticks(1,x.min,x.max,5);
}

# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

R
   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f, $match_f, $query_f);

   return @Rv;
}

#,=======================================================================
# ---------------------------- End of Package ----------------------------
#,=======================================================================

1;

