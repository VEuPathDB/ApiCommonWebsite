
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiRaw;
@ISA = qw( ApiCommonWebsite::View::GraphPackage );

=pod

=head1 Summary

Makes the plots for the raw data in deRisi developmental time series
experiments.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;
	 my $Args = ref $_[0] ? shift : {@_};

   $Self->SUPER::init($Args);

	 $Self->setRedQuery             ( $Args->{RedQuery            } );
	 $Self->setGreenQuery           ( $Args->{GreenQuery          } );

   #$Self->setRedPercentileQuery   ( $Args->{RedPercentileQuery  } );
   #$Self->setGreenPercentileQuery ( $Args->{GreenPercentileQuery} );

   return $Self;
}

# ------------------------------ Accessors -------------------------------

sub getRedQuery             { $_[0]->{'RedQuery'          } }
sub setRedQuery             { $_[0]->{'RedQuery'          } = $_[1]; $_[0] }

sub getGreenQuery           { $_[0]->{'GreenQuery'        } }
sub setGreenQuery           { $_[0]->{'GreenQuery'        } = $_[1]; $_[0] }

#sub getRedPercentileQuery   { $_[0]->{'RedPercentileQuery'} }
#sub setRedPercentileQuery   { $_[0]->{'RedPercentileQuery'} = $_[1]; $_[0] }

#sub getGreenPercentileQuery { $_[0]->{'GreenPercentileQuery'} }
#sub setGreenPercentileQuery { $_[0]->{'GreenPercentileQuery'} = $_[1]; $_[0] }

# ========================================================================
# ------------------------------- Methods --------------------------------
# ========================================================================

sub makeR {
   my $Self = shift;

   my @Rv;

   my $id = $Self->getId();

   my $_qh   = $Self->getQueryHandle();
   my $_dict = {};

	 my $fmt        = $Self->getFormat();
   my $r_f        = $Self->getOutputFile(). '.R';
   my $out_f      = $Self->getOutputFile();

   my $r_fh = FileHandle->new(">$r_f") ||
   die "Can not open R file '$r_f': $!";

   my @errors = ();

   my $redRaw_f   = eval { $Self->getRedQuery(  )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $greenRaw_f = eval { $Self->getGreenQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $redPct_f   = eval { $Self->getRedQuery(  )->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
   my $greenPct_f = eval { $Self->getGreenQuery()->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);

   if (@errors) {
      $Self->reportErrorsAndBlankGraph($r_fh, @errors);
   }

   else {
      my $open_R     = $Self->rOpenFile(480,360);

      my $preamble_R = $Self->_rStandardComponents();

      print $r_fh <<R;

# ------------------------------- Prepare --------------------------------

$preamble_R

# ------------------------------ Load Data -------------------------------

data.red.raw     <- read.table("$redRaw_f",     header=T);
data.green.raw   <- read.table("$greenRaw_f",   header=T);

data.red.pct     <- read.table("$redPct_f",     header=T);
data.green.pct   <- read.table("$greenPct_f",   header=T);

ylim.min = min(-2, data.green.raw\$VALUE, data.red.raw\$VALUE);
ylim.max = max( 2, data.green.raw\$VALUE, data.red.raw\$VALUE);

# ------------------------------- Plotting -------------------------------

$open_R;

plasmodb.par();

screen.dims <- t(array(c(c(0.0, 1.0, 0.00, 1.00),
                        ),
                       dim=c(4,1)
                       )
                 );
screens     <- split.screen(screen.dims, erase=T);

# ------------------------------- Raw Data -------------------------------

# green data, red data, nice ticks, and a grid.

screen(screens[1]);
plasmodb.par.last();
plot(data.green.raw\$ELEMENT_ORDER,
     data.green.raw\$VALUE,
     col  = "green",
     bg   = "green",
     type = "o",
     pch  = 22,
     xlab = "Time Point",
     ylab = "Raw Expression Value",
     ylim = c(ylim.min, ylim.max)
    );
lines(data.red.raw\$ELEMENT_ORDER,
      data.red.raw\$VALUE,
      col  = "red",
      bg   = "red",
      type = "o",
      pch  = 22
     );
plasmodb.ticks(1, 0, nrow(data.red.raw), 5);
plasmodb.grid();

## ---------------------- SCREEN 2 : Percentile Data ----------------------
#
#screen(screens[2]);
#plasmodb.par.last();
#plot(data.green.pct\$ELEMENT_ORDER,
#     data.green.pct\$VALUE,
#     col  = "green",
#     bg   = "green",
#     type = "o",
#     pch  = 22,
#     xlab = "Time Point",
#     ylab = "Pct Expression Value",
#     ylim = c(0,100)
#    );
#lines(data.red.pct\$ELEMENT_ORDER,
#      data.red.pct\$VALUE,
#      col  = "red",
#      bg   = "red",
#      type = "o",
#      pch  = 22,
#     );
#plasmodb.ticks(1,0,nrow(data.red.pct),5);
#plasmodb.grid();

# --------------------------------- Done ---------------------------------
dev.off();
quit(save="no")

R

   }

   $r_fh->close();

   push(@Rv, $r_f, $out_f, $redRaw_f, $greenRaw_f );

   return @Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
