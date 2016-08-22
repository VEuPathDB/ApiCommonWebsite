package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Llinas::pHMetabolite;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);

  my $compoundId = $self->getId();

  my $colors = ['dodgerblue', 'slateblue', 'forestgreen', '#2F4F4F', 'salmon', '#E9967A'];

  my $elementNames = ['', 'pp6.4', 'pp7.4', 'pp8.4', 'rp6.4', 'rp7.4', 'rp8.4', 'sp6.4', 'sp7.4', 'sp8.4', '', 'pm6.4', 'pm7.4', 'pm8.4', 'rm6.4', 'rm7.4', 'rm8.4', 'sm6.4', 'sm7.4', 'sm8.4'];

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

  my $massSpec = ApiCommonWebsite::View::GraphPackage::BarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
    zeros = rep(0, nrow(profile.df));
    zeros_start = rep(c(0:5), time=3);
    zeros_end = rep(c(5:0), time=3);
    the.colors = rep(the.colors, each=nrow(profile.df));

    #reorder dataframe
    profile.df2 = profile.df[, c(1,4,7,10,13,16,2,5,8,11,14,17,3,6,9,12,15,18)];
    profile.df = as.data.frame(matrix(nrow=6*nrow(profile.df2)));
    profile.df$V1 = NULL;

    #fill out dataframe with zeros to make grouping work
    for (i in 1:ncol(profile.df2)) {
        newcol = c(rep(zeros, zeros_start[i]), profile.df2[,i], rep(zeros, zeros_end[i]));
        profile.df = cbind(profile.df, newcol);
    }

    names(profile.df) = names(profile.df2);

    #stderr is null for this graph - make dummy df same size as profile.df
    stderr.df = as.data.frame(matrix(nrow=nrow(profile.df), ncol=ncol(profile.df)));
    names(stderr.df) = names(profile.df);
RADJUST
  $massSpec->setAdjustProfile($rAdjustString);
  $massSpec->setProfileSets($profileSets);
  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);
  $massSpec->setIsStacked(1);
  $massSpec->setSampleLabels(['','','','6.4','','','','','','7.4','','','','','','8.4','','']);
  $massSpec->setSpaceBetweenBars("c(0.75, rep(0, time=5))");
  $massSpec->setAxisLty(0);
  $massSpec->setLas(0);
  $massSpec->setLabelCex(0.9);
  $massSpec->setHasExtraLegend(1);
  $massSpec->setExtraLegendSize(8.0);
  $massSpec->setLegendColors($colors);
  $massSpec->setLegendLabels(['Percoll pellet', 'Percoll media', 'Saponin pellet', 'Saponin media', 'Uninfected RBC pellet', 'Uninfected RBC media']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
