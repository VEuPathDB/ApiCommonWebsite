
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
SELECT distinct pan.node_order_num, pan.name
FROM   study.protocolAppNode pan, study.study s, study.studylink sl
WHERE  s.name            = '<<ProfileSet>>'
AND    s.study_id = sl.study_id
AND    pan.protocol_app_node_id = sl.protocol_app_node_id
ORDER  BY pan.node_order_num
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

   my $elementOrder = $Self->getElementOrder();

   my @Rv;

   # prepare dictionary
   $Dict = $Self->prepareDictionary($Dict);

   # execute SQL and get result
   my $_sql = $Self->getExpandedSql($Dict);
   my $_sh  = $Qh->prepare($_sql);
   $_sh->execute();


   my $i = 0;
     while (my $_row = $_sh->fetchrow_hashref()) {
       my $eo = $elementOrder->[$i];

       if(scalar @$elementOrder > 0) {
         $_row->{NAME} = $eo;
         push(@Rv, $_row) if($eo);
       }
       else {
         push(@Rv, $_row);
       }
       #$Rv[$_row->{ELEMENT_ORDER}]->{NAME} = $_row->{NAME};

       $i++;
     }
   $_sh->finish();

   return wantarray ? @Rv : \@Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;




