package ApiCommonWebsite::View::GraphPackage::EuPathDB::PathwayGenera;

use strict;
use vars qw( @ISA );

use ApiCommonWebsite::Model::CannedQuery::PathwayGeneraNames;
use ApiCommonWebsite::Model::CannedQuery::PathwayGeneraData;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  my $generaList = ['Babesia',  'Eimeria', 'Leishmania', 'Trypanosoma'];
  my $listOfLists = $self->makeListOfLists();

  my @generaList = map { $_->[0] } @$listOfLists;
  my @colorList = map { $_->[2] } @$listOfLists;

  my $names = ApiCommonWebsite::Model::CannedQuery::PathwayGeneraNames->new
      ( Name         => "_names",
        Genera => \@generaList,
      );

  my $data = ApiCommonWebsite::Model::CannedQuery::PathwayGeneraData->new
        ( Name         => "_data",
          Genera => \@generaList,
          Id => $self->getId(),
        );

  my $profileSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new("INTERNAL", [], undef, undef, undef, "Display Name");
  $profileSet->setProfileCannedQuery($data);
  $profileSet->setProfileNamesCannedQuery($names);

  my $profileSets = [$profileSet];

  my $genera = ApiCommonWebsite::View::GraphPackage::BarPlot::Genera->new(@_);
  $genera->setProfileSets($profileSets);
  $genera->setDataObject($data);
  $genera->setNamesObject($names);
  $genera->setElementNameMarginSize(7.5);

  $genera->setColors(\@colorList);

  $self->setGraphObjects($genera);

  return $self;
}

sub makeListOfLists {
  my ($self) = @_;

  my $sid = $self->getSecondaryId();

  my @genera = split(/,/, $sid);
  my %genera;
  foreach(@genera) {
    $genera{$_} = 1;
  }

  my @superset = (['Acanthamoeba', 'AmoebaDB', '#6F4E37',],
                  ['Entamoeba', 'AmoebaDB','#6F4E37', ],
                  ['Naegleria', 'AmoebaDB','#6F4E37', ],

                  ['Cryptosporidium', 'CryptoDB', '#2554C7', ],
                  ['Chromera', 'CryptoDB', '#777777'], #
                  ['Vitrella', 'CryptoDB', '#777777'], #

                  ['Eimeria', 'ToxoDB', '#59E817',],
                  ['Gregarina', 'ToxoDB', '#59E817',],
                  ['Neospora', 'ToxoDB', '#59E817',],
                  ['Toxoplasma', 'ToxoDB', '#59E817',],

                  ['Plasmodium', 'PlasmoDB', '#A74AC7'], #

                  ['Babesia','PiroplasmaDB', '#3BB9FF', ],  #
                  ['Theileria', 'PiroplasmaDB', '#3BB9FF',], #

                  ['Giardia', 'GiardiaDB', '#667C26'], #
                  ['Spironucleus', 'GiardiaDB', '#667C26'], #

                  ['Crithidia', 'TriTrypDB', '#F87217', ], #
                  ['Leishmania', 'TriTrypDB', '#F87217', ], #
                  ['Trypanosoma', 'TriTrypDB','#F87217', ], #

                  ['Anncaliia', 'MicrosporidiaDB', '#461B7E'],
                  ['Edhazardia', 'MicrosporidiaDB', '#461B7E'],
                  ['Encephalitozoon', 'MicrosporidiaDB', '#461B7E'],
                  ['Enterocytozoon', 'MicrosporidiaDB', '#461B7E'],
                  ['Nematocida', 'MicrosporidiaDB', '#461B7E'],
                  ['Nosema', 'MicrosporidiaDB', '#461B7E'],
                  ['Spraguea', 'MicrosporidiaDB', '#461B7E'],
                  ['Vavraia', 'MicrosporidiaDB', '#461B7E'],
                  ['Vittaforma', 'MicrosporidiaDB', '#461B7E'],

                  ['Schistosoma', 'SchistoDB', '#2554C7', ],

		  ['Aspergillus','FungiDB', '#2554C7'],
		  ['Phytophthora','FungiDB', '#2554C7'],
		  ['Pythium','FungiDB', '#2554C7'],
		  ['Aphanomyces','FungiDB', '#2554C7'],
		  ['Saprolegnia','FungiDB', '#2554C7'],
		  ['Neurospora','FungiDB', '#2554C7'],
		  ['Albugo','FungiDB', '#2554C7'],
		  ['Fusarium','FungiDB', '#2554C7'],
		  ['Coccidioides','FungiDB', '#2554C7'],
		  ['Talaromyces','FungiDB', '#2554C7'],

                  ['Trichomonas', 'TrichDB', '#78866B'],

                  ['Homo', 'HostDB', '#00FFFF'],
                  ['Mus', 'HostDB', '#00FFFF']


      );

  my @rv;
  foreach(@superset) {
    my $g = $_->[0];
    push @rv, $_ if($genera{$g});
  }
  return \@rv;
}


1;
