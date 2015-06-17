package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DwellingLightTrap;

use strict;
use vars qw( @ISA );

use ApiCommonWebsite::Model::CannedQuery::DwellingLightTrapData;
use ApiCommonWebsite::Model::CannedQuery::DwellingLightTrapNames; 


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet; #Generic super class, does not change
use ApiCommonWebsite::View::GraphPackage::BarPlot; #BarPlot

sub init {
  my $self = shift;
  my $args = ref $_[0] ? shift : {@_}; #These two lines before init
   
  $self->SUPER::init(@_); #Run init method of super class, then set varialbes local to your class
  
  #Variables local to your class
  $self->setId    ( $args->{Id                  } );
  $self->setStartDate($args->{StartDate});
  $self->setEndDate($args->{EndDate});


  

  my $data = ApiCommonWebsite::Model::CannedQuery::DwellingLightTrapData->new
        ( Name         => "_lighttrapdata",
          Id => $self->getId,
          StartDate => $self->getStartDate(),
          EndDate => $self->getEndDate()
        );

  my $names = ApiCommonWebsite::Model::CannedQuery::DwellingLightTrapNames->new
      ( Name         => "_names",
        Id => $self->getId(),
        StartDate => $self->getStartDate(),
        EndDate => $self->getEndDate()
      );

  #my $size = scalar($names->getValues());
  #my $i=0;
  #my @graphColors; $graphColors[$size] = ();
 # for($i=0;$i<$size;$i++){
  # $graphColors[$i] = "#000FFF";
  #      } 

  my @internalProfileSetNames = (["_INTERNAL"]);
  my $internalProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@internalProfileSetNames);

  my $lt = ApiCommonWebsite::View::GraphPackage::BarPlot::LightTrap->new(@_);
  $lt->setProfileSets($internalProfileSets);
  $lt->setDataObject($data);
  $lt->setNamesObject($names);
  $lt->setElementNameMarginSize(7.5);

  #$lt->setColors(\@graphColors);

  $self->setGraphObjects($lt);

  return $self;
}

#declare outside of the init as these are now methods
sub getId                   { $_[0]->{'Id'                } }
sub setId                   { $_[0]->{'Id'                } = $_[1]; $_[0] }

sub setStartDate { $_[0]->{_startdate} = $_[1] }
sub getStartDate { $_[0]->{_startdate} }

sub setEndDate { $_[0]->{_enddate} = $_[1] }
sub getEndDate { $_[0]->{_enddate} }




1;
