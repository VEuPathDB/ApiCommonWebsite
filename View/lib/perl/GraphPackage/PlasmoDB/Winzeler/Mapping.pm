package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

=pod

=head1 Summary

This package contains the mappings from stages to time points in the
DeRisi time course.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

# ========================================================================
# --------------------------- Global Variables ---------------------------
# ========================================================================

my @temperature_map = ( 'ER' => 4,
                        'LR' => 10,
                        'ET' => 17,
                        'LT' => 25,
                        'ES' => 31,
                        'LS' => 38,
                        'VLS' => 48,
                      );

my @sorbitol_map    = ( 'ER' => 7,
                        'LR' => 13,
                        'ET' => 21,
                        'LT' => 28,
                        'ES' => 34,
                        'LS' => 40,
                      );

# ========================================================================
# ------------------------------ Functions -------------------------------
# ========================================================================

sub TemperatureTimes {
   my @Rv = _toList(\@temperature_map, 1);

   return wantarray ? @Rv : \@Rv;
}

sub SorbitolTimes {
   my @Rv = _toList(\@sorbitol_map, 1);

   return wantarray ? @Rv : \@Rv;
}

sub TemperatureStages {
   my @Rv = _toList(\@temperature_map, 0);

   return wantarray ? @Rv : \@Rv;
}

sub SorbitolStages {
   my @Rv = _toList(\@sorbitol_map, 0);

   return wantarray ? @Rv : \@Rv;
}

sub TemperatureMap {
   my %Rv = @temperature_map;

   return wantarray ? %Rv : \%Rv;
}

sub SorbitolMap {
   my %Rv = @sorbitol_map;

   return wantarray ? %Rv : \%Rv;
}

# ========================================================================
# -------------------------- Private Functions ---------------------------
# ========================================================================

sub _toList {
   my $List  = shift;
   my $Start = shift;

   my @Rv;

   for (my $i = $Start; $i < @$List; $i += 2) {
      push(@Rv, $List->[$i]);
   }

   return wantarray ? @Rv : \@Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
