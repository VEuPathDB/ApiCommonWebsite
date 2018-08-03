package ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGLinePlot;

use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;

use EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues;
use EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthNames;


sub getPhenotypeSpecs { }

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $phenotypeSpecs = $self->getPhenotypeSpecs();

  my @gos;
  foreach my $ps (@$phenotypeSpecs) {
    my $go = $self->makePhenotypeGraphObject($ps->{query}, $ps->{abbrev}, $ps->{name}, $ps->{postscript});
    push @gos, $go;
  }

  $self->setGraphObjects(@gos);

  return $self;

}

sub makePhenotypeGraphObject {
  my ($self, $sourceIdValueQuery, $abbrev, $name, $postscript) = @_;

  my $id = $self->getId();

  my $goValuesCannedQueryGene = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 200, Name => "_${abbrev}_gv", Id => $id);

  my $goNamesCannedQueryGene = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthNames->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 200, Name => "_${abbrev}_gen", Id => $id);


  my $goValuesCannedQueryCurve = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 200, Name => "_${abbrev}_av", Id => 'ALL');

  my $goNamesCannedQueryCurve = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthNames->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 200, Name => "_${abbrev}_aen", Id => 'ALL');


  my $goProfileSetGene = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetGene->setProfileCannedQuery($goValuesCannedQueryGene);
  $goProfileSetGene->setProfileNamesCannedQuery($goNamesCannedQueryGene);

  my $goProfileSetCurve = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetCurve->setProfileCannedQuery($goValuesCannedQueryCurve);
  $goProfileSetCurve->setProfileNamesCannedQuery($goNamesCannedQueryCurve);

  my $go = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);

  $go->setDefaultYMin(0);
  $go->setProfileSets([$goProfileSetCurve, $goProfileSetGene]);
  $go->setYaxisLabel($name);
  $go->setColors(["grey", "blue"]);
  $go->setPartName($abbrev);
  $go->setPlotTitle("$id - $name");
  $go->setXaxisLabel("");

  $go->setRPostscript($postscript) if($postscript && !$self->getCompact());

  my $legend = ["All Genes", $id];

  $go->setHasExtraLegend(1);
  $go->setLegendLabels($legend);


  return $go;
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore::DS_1cc763e9d0;
use strict;
use vars qw( @ISA );

@ISA = qw(ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore );

sub getPhenotypeSpecs {
  return [ {abbrev => "MIS",
            name => "Mutagenesis Index Score",
            query => "select ga.source_id, r.score as value
                      from apidb.phenotypescore r, apidbtuning.geneattributes ga 
                      where ga.na_feature_id = r.na_feature_id
                      and r.score_type = 'mutagenesis index score'",
            postscript => "gp = gp + annotate(\"text\", x = 500, y = 0.05, label = \"Essential\", colour = 'red');
gp = gp + annotate(\"text\", x = 5000, y = 0.9, label = \"Dispensable\", colour = '#d3883f');"

           },
           {abbrev => "MFS",
            name => "Mutant Fitness Score",
            query => "select ga.source_id, r.score as value
                               from apidb.phenotypescore r, apidbtuning.geneattributes ga 
                               where ga.na_feature_id = r.na_feature_id
                               and r.score_type = 'mutant fitness score'"
           },
      ];
}

sub declareParts {
  my ($self) = @_;

  my $myPlotParts = $self->SUPER::declareParts();
  #my $oldNewPlotPart = @{$myPlotParts}[0];
  #push @{$myPlotParts}, $oldNewPlotPart;
  pop @{$myPlotParts};

  @{$myPlotParts}[0]->{visible_part} = "MIS,MFS";
  @{$myPlotParts}[0]->{height} = @{$myPlotParts}[0]->{height} + 300;

  return $myPlotParts;
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore::DS_90eea17ef6;
use strict;
use vars qw( @ISA );

@ISA = qw(ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore );

sub getPhenotypeSpecs {
  return [ {abbrev => "rel_growth_rate",
            name => "Relative Growth Rate",
            query => "select ga.source_id
                           , r.relative_growth_rate as value
                      from APIDB.PHENOTYPEGROWTHRATE r
                         , apidbtuning.geneattributes ga 
                      where r.na_feature_id = ga.na_feature_id"
           },
      ];
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore::DS_d4745ea297;
use strict;
use vars qw( @ISA );

@ISA = qw(ApiCommonWebsite::View::GraphPackage::Templates::PhenotypeScore );

sub getPhenotypeSpecs {
  return [ {abbrev => "phenotype_score",
            name => "Phenotype Score",
            query => "select ga.source_id, r.mean_phenotype as value
                      from APIDB.CRISPRPHENOTYPE r,
                           apidbtuning.geneattributes ga
                      where ga.na_feature_id = r.na_feature_id",
            postscript => "gp = gp + annotate(\"text\", x = 1500, y = -6, label = \"Fitness Conferring\", colour = 'red');
gp = gp + annotate(\"text\", x = 7000, y = 2.5, label = \"Dispensable\", colour = '#d3883f');"
           },
      ];
}

1;
