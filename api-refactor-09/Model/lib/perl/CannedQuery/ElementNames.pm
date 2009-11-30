
package ApiCommonWebsite::Model::CannedQuery::ElementNames;
@ISA = qw( ApiCommonWebsite::Model::CannedQuery );

=pod

=head1 Purpose

This canned query selects the element names for a profileSet with a
given name.

=head1 Macros

The following macros must be available to execute this query.

=over

=item Profile - source id of the profile set.

=back

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use FileHandle;

use ApiCommonWebsite::Model::CannedQuery;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;
  my $Args = ref $_[0] ? shift : {@_};

  $Self->SUPER::init($Args);

	$Self->setProfileSet           ( $Args->{ProfileSet          } );

  $Self->setSql(<<Sql);
SELECT pen.element_order, pen.name
FROM   apidb.ProfileSet         ps
,      apidb.ProfileElementName pen
WHERE  ps.name            = '<<ProfileSet>>'
AND    pen.profile_set_id = ps.profile_set_id
ORDER  BY pen.element_order
Sql

  return $Self;
}

# -------------------------------- access --------------------------------

sub getProfileSet           { $_[0]->{'ProfileSet'        } }
sub setProfileSet           { $_[0]->{'ProfileSet'        } = $_[1]; $_[0] }

# ========================================================================
# --------------------------- Support Methods ----------------------------
# ========================================================================

sub prepareDictionary {
	 my $Self = shift;
	 my $Dict = shift || {};

	 my $Rv = $Dict;

	 $Dict->{ProfileSet} = $Self->getProfileSet();

	 return $Rv;
}

# ------------------------------ getValues -------------------------------

=pod

=head2 getValues

Override super class getValues call.

=cut

sub getValues {
   my $Self = shift;
	 my $Qh   = shift;
	 my $Dict = shift;

   my @Rv;

	 # prepare dictionary
	 $Dict = $Self->prepareDictionary($Dict);

	 # execute SQL and get result
	 my $_sql = $Self->getExpandedSql($Dict);
	 my $_sh  = $Qh->prepare($_sql);
   $_sh->execute();
	 while (my $_row = $_sh->fetchrow_hashref()) {
      push(@Rv, $_row);
      #$Rv[$_row->{ELEMENT_ORDER}]->{NAME} = $_row->{NAME};
   }
   $_sh->finish();

   return wantarray ? @Rv : \@Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;




