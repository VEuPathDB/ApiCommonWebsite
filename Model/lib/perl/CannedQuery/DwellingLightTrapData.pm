package ApiCommonWebsite::Model::CannedQuery::DwellingLightTrapData;
@ISA = qw( ApiCommonWebsite::Model::CannedQuery );

use strict;

use Data::Dumper;

use ApiCommonWebsite::Model::CannedQuery;

sub init {
  my $Self = shift;
  my $Args = ref $_[0] ? shift : {@_};

  $Self->SUPER::init($Args);

  $Self->setId                   ( $Args->{Id                  } );
  $Self->setStartDate($Args->{StartDate});
  $Self->setEndDate($Args->{EndDate});



  

  $Self->setSql(<<Sql);
SELECT   
lta.total_anopheles as value
FROM APIDBTUNING.DWELLINGATTRIBUTES da, APIDBTUNING.LIGHTTRAPATTRIBUTES lta
where da.source_id=lta.PARENT_ID
and da.source_id='<<Id>>'
and TO_DATE(lta.COLLECTION_DATE) between TO_DATE('<<StartDate>>', 'DD-MM-YYYY') and TO_DATE('<<EndDate>>', 'DD-MM-YYYY')
order by lta.monthyear
Sql

  return $Self;
}


sub getId                   { $_[0]->{'Id'                } }
sub setId                   { $_[0]->{'Id'                } = $_[1]; $_[0] }

sub setStartDate { $_[0]->{_startdate} = $_[1] }
sub getStartDate { $_[0]->{_startdate} }

sub setEndDate { $_[0]->{_enddate} = $_[1] }
sub getEndDate { $_[0]->{_enddate} }



sub prepareDictionary {
	 my $Self = shift;
	 my $Dict = shift || {};

	 my $Rv = $Dict; #Rv - return value

	 $Dict->{Id}         =  $Self->getId();
	 $Dict->{StartDate}         =  $Self->getStartDate();
	 $Dict->{EndDate}         =  $Self->getEndDate();

         
	 return $Rv;
}


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

   my $countNonZero;
     while (my $_row = $_sh->fetchrow_hashref()) {
       push(@Rv, $_row);

       $countNonZero++ if($_row->{VALUE});
     }
   $_sh->finish();

   unless($countNonZero) {
     die "No genera w/ matching EC";
   }

   return wantarray ? @Rv : \@Rv;
}


1;
