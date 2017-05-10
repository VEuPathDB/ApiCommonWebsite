package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Llinas::pHMetabolite;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::GGBarPlot;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  # Facet override (put before init for default)
  $self->setFacets(["pH"]);

  $self->setPlotWidth(600);

  my $compoundId = $self->getId();

  my $colors = ['dodgerblue', 'slateblue', 'forestgreen', '#2F4F4F', 'salmon', '#E9967A'];

  my $elementNames = ['', 'Percoll pellet|6.4', 'Percoll pellet|7.4', 'Percoll pellet|8.4', 'Uninfected RBC pellet|6.4', 'Uninfected RBC pellet|7.4', 'Uninfected RBC pellet|8.4', 'Saponin pellet|6.4', 'Saponin pellet|7.4', 'Saponin pellet|8.4', '', 'Percoll media|6.4', 'Percoll media|7.4', 'Percoll media|8.4', 'Uninfected RBC media|6.4', 'Uninfected RBC media|7.4', 'Uninfected RBC media|8.4', 'Saponin media|6.4', 'Saponin media|7.4', 'Saponin media|8.4'];

  my $dbh = $self->getQueryHandle();

    # some compounds have duplicate rows where the isotopomer is C12 and null
    # if both exist, take only the C12 row
    my $sql =   "WITH iso AS (
                    SELECT DISTINCT cms.isotopomer
                    FROM results.compoundmassspec cms
                    , study.protocolappnode pan
                    , chebi.compounds c
                    WHERE c.chebi_accession = '$compoundId'
                    AND cms.compound_id = c.id
                    AND cms.protocol_app_node_id = pan.protocol_app_node_id
                    )
                SELECT DISTINCT
                CASE WHEN 'C12' in (SELECT * from iso)
                    THEN nvl(isotopomer, 'C12')
                    ELSE isotopomer
                    END AS isotopomer
                FROM (SELECT * FROM iso)";
            


  my $sh = $dbh->prepare($sql);
  $sh->execute();

  my @profileSetNames;

  while (my ($isotopomer) = $sh->fetchrow_array()) {
    my $profileSetName = ['Profiles of Metabolites from Llinas', 'values', '', '', $elementNames, '', '', '', '', $isotopomer];
    push (@profileSetNames, $profileSetName);
  }

  # @profileSetNames needs to be populated even if there is no data
  push (@profileSetNames, ['Profiles of Metabolites from Llinas', 'values']) unless (@profileSetNames);

  $sh->finish();
    
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $massSpec = ApiCommonWebsite::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
profile.df.full$LEGEND=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,1];
profile.df.full$pH=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,2];
profile.df.full$NAME = factor(profile.df.full$LEGEND, levels=legend.label); 
RADJUST
  $massSpec->setAdjustProfile($rAdjustString);
  $massSpec->setProfileSets($profileSets);
#  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);

 # $massSpec->setSampleLabels(['','','','6.4','','','','','','7.4','','','','','','8.4','','']);
 # $massSpec->setSpaceBetweenBars("c(0.75, rep(0, time=5))");
 # $massSpec->setAxisLty(0);
 # $massSpec->setLas(0);
 # $massSpec->setHasExtraLegend(1);
#  $massSpec->setExtraLegendSize(8.0);

  $massSpec->setIsStacked(1);
#  $massSpec->setLegendColors($colors);
  $massSpec->setLegendLabels(['Percoll pellet', 'Percoll media', 'Saponin pellet', 'Saponin media', 'Uninfected RBC pellet', 'Uninfected RBC media']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
