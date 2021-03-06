package ApiCommonWebsite::View::GraphPackage::EuPathDB::PathwayGenera;

use strict;
use vars qw( @ISA );

use EbrcWebsiteCommon::Model::CannedQuery::PathwayGeneraNames;
use EbrcWebsiteCommon::Model::CannedQuery::PathwayGeneraData;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;
 
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  my $generaList = ['Babesia',  'Eimeria', 'Leishmania', 'Trypanosoma'];
  my $listOfLists = $self->makeListOfLists();

  my @generaList = map { $_->[0] } @$listOfLists;
  my @colorList = map { $_->[2] } @$listOfLists;

  my $i = 1;
  my $generaSql = join (" union ", map { "select '$_' as genus, " . $i++ . " as o from dual"  } @generaList);
  print STDERR Dumper("genera sql: " . $generaSql);

  my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSet->setJsonForService("{\"generaSql\":\"$generaSql\"}");
  $profileSet->setSqlName("PathwayGenera");

  my $profileSets = [$profileSet];

  my $genera = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Genera->new(@_);
  $genera->setProfileSets($profileSets);
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

                  ['Besnoitia', 'ToxoDB', '#59E817'],
                  ['Cyclospora', 'ToxoDB', '#59E817'],
                  ['Cystoisospora', 'ToxoDB', '#59E817'],
                  ['Eimeria', 'ToxoDB', '#59E817',],
                  ['Gregarina', 'ToxoDB', '#59E817',],
                  ['Hammondia', 'ToxoDB', '#59E817',],
                  ['Neospora', 'ToxoDB', '#59E817',],
                  ['Sarcocystis', 'ToxoDB', '#59E817',],
                  ['Toxoplasma', 'ToxoDB', '#59E817',],

                  ['Hepatocystis', 'PlasmoDB', '#A74AC7'],
                  ['Plasmodium', 'PlasmoDB', '#A74AC7'], #

                  ['Babesia','PiroplasmaDB', '#3BB9FF', ],  #
                  ['Theileria', 'PiroplasmaDB', '#3BB9FF',], #

                  ['Giardia', 'GiardiaDB', '#667C26'], #
                  ['Spironucleus', 'GiardiaDB', '#667C26'], #

                  ['Crithidia', 'TriTrypDB', '#F87217', ], #
                  ['Leishmania', 'TriTrypDB', '#F87217', ], #
                  ['Trypanosoma', 'TriTrypDB','#F87217', ], #
                  ['Blechomonas', 'TriTrypDB','#F87217', ],
                  ['Bodo', 'TriTrypDB','#F87217', ],
                  ['Endotrypanum', 'TriTrypDB','#F87217', ],
                  ['Leptomonas', 'TriTrypDB','#F87217', ],
                  ['Paratrypanosoma', 'TriTrypDB','#F87217', ],

                  ['Anncaliia', 'MicrosporidiaDB', '#461B7E'],
                  ['Edhazardia', 'MicrosporidiaDB', '#461B7E'],
                  ['Encephalitozoon', 'MicrosporidiaDB', '#461B7E'],
                  ['Enterocytozoon', 'MicrosporidiaDB', '#461B7E'],
                  ['Nematocida', 'MicrosporidiaDB', '#461B7E'],
                  ['Nosema', 'MicrosporidiaDB', '#461B7E'],
                  ['Spraguea', 'MicrosporidiaDB', '#461B7E'],
                  ['Vavraia', 'MicrosporidiaDB', '#461B7E'],
                  ['Vittaforma', 'MicrosporidiaDB', '#461B7E'],

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
                  ['Mucor','FungiDB', '#2554C7'],
                  ['Phycomyces','FungiDB', '#2554C7'],
                  ['Rhizophagus','FungiDB', '#2554C7'],
                  ['Candida','FungiDB', '#2554C7'],
                  ['Clavispora','FungiDB', '#2554C7'],
                  ['Hanseniaspora','FungiDB', '#2554C7'],
                  ['Saccharomyces','FungiDB', '#2554C7'],
                  ['Yarrowia','FungiDB', '#2554C7'],
                  ['Schizosaccharomyces','FungiDB', '#2554C7'],
                  ['Lomentospora','FungiDB', '#2554C7'],
                  ['Pyricularia','FungiDB', '#2554C7'],
                  ['Scedosporium','FungiDB', '#2554C7'],
                  ['Sordaria','FungiDB', '#2554C7'],
                  ['Sporothrix','FungiDB', '#2554C7'],
                  ['Thermothelomyces','FungiDB', '#2554C7'],
                  ['Trichoderma','FungiDB', '#2554C7'],
                  ['Cryptococcus','FungiDB', '#2554C7'],
                  ['Kwoniella','FungiDB', '#2554C7'],
                  ['Tremella','FungiDB', '#2554C7'],
                  ['Sporisorium','FungiDB', '#2554C7'],
                  ['Ustilago','FungiDB', '#2554C7'],
                  ['Hyaloperonospora','FungiDB', '#2554C7'],
                  ['Globisporangium','FungiDB', '#2554C7'],
                  ['Phytopythium','FungiDB', '#2554C7'],

                  ['Trichomonas', 'TrichDB', '#78866B'],

                  ['Homo', 'HostDB', '#00FFFF'],
                  ['Macaca', 'HostDB', '#00FFFF'],
                  ['Mus', 'HostDB', '#00FFFF'],
                  ['Bos', 'HostDB', '#00FFFF'],

                  ['Lutzomyia', 'VectorBase', '#228B22'],
                  ['Leptotrombidium', 'VectorBase', '#228B22'],
                  ['Cimex', 'VectorBase', '#228B22'],
                  ['Culicoides', 'VectorBase', '#228B22'],
                  ['Drosophila', 'VectorBase', '#228B22'],
                  ['Pediculus', 'VectorBase', '#228B22'],
                  ['Culex', 'VectorBase', '#228B22'],
                  ['Glossina', 'VectorBase', '#228B22'],
                  ['Anopheles', 'VectorBase', '#228B22'],
                  ['Aedes', 'VectorBase', '#228B22'],
                  ['Musca', 'VectorBase', '#228B22'],
                  ['Rhodnius', 'VectorBase', '#228B22'],
                  ['Stomoxys', 'VectorBase', '#228B22'],
                  ['Phlebotomus', 'VectorBase', '#228B22'],
                  ['Sarcoptes', 'VectorBase', '#228B22'],
                  ['Biomphalaria', 'VectorBase', '#228B22'],
                  ['Ixodes', 'VectorBase', '#228B22'],

      );

  my @rv;
  foreach(@superset) {
    my $g = $_->[0];
    push @rv, $_ if($genera{$g});
  }
  return \@rv;
}


1;
