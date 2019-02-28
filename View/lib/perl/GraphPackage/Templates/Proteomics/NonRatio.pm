package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

sub restrictProfileSetsBySourceId { return 1;}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::tgonME49_quantitativeMassSpec_Wastling_strain_timecourses_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio );
use strict;

sub setMainLegend {
  my $self = @_;

  my $colors = [ '#144BE5' , '#70598F' , '#5B984D' , '#FA9B83' , '#EF724E' , '#E1451A' ];

  print STDERR "Got here, but it's not working";
  my $pch = [ '15', '16', '17', '18', '7:10', '0:6'];

  my $legend = ['GT1 16 hr time course','ME49 16 hr time course', 'ME49 44 hr time course', 'RH 36 hr time course', 'VEG 16 hr time course', 'VEG 44 hr time course'];


  my $hash = {colors => [ '#E9967A', '#87CEFA', '#00BFFF','#4169E1', '#0000FF', ], short_names => $legend, points_pch => $pch, cols=> 2};

  $self->SUPER::setMainLegend($hash);
}

1;

# for ToxoDB


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_3c48f52edb;
use Data::Dumper;

sub getRemainderRegex {
  return qr/T\. ?gondii ?(.+) timecourse/;
}

sub keepSingleLegend {1}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setHasExtraLegend(1);
print STDERR Dumper($profile->getLegendLabels());
  if($profile->getLegendLabels()) {
    my @legendLabels = map {s/Quantitative protein expression of Tgondii proteins in infection of human cells - //;$_} @{$profile->getLegendLabels()};
    $profile->setLegendLabels(\@legendLabels);
    my $colorMap = "c(\"GT1 0 to 16 hour\" = \"#144BE5\", \"ME49 0 to 16 hour\" = \"#70598F\", \"ME49 0 to 44 hour\" = \"#5B984D\", \"RH 0 to 36 hour\" = \"#FA9B83\", \"VEG 0 to 16 hour\" = \"#EF724E\", \"VEG 0 to 44 hour\" = \"#E1451A\")";

    $profile->setColorVals($colorMap);
  }

  return $self;
}
1;


# for HostDB
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_08fe07cd15;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $legend = ['GT1 0 to 16 hr','ME49 0 to 16 hr','ME49 0 to 44 hr', 'RH 0 to 36 hr',
		'VEG 0 to 16 hr', 'VEG 0 to 44 hr'];
  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legend);
  return $self;
}
1;


# for TriTrypDB
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_bf9c234fd9;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setDefaultYMax(0.4);
  $profile->addAdjustProfile('profile.df.full = transform(profile.df.full, "LEGEND" = ifelse(NAME == "0.5 hrs" | NAME == " 3 hrs" | NAME == " 10 hrs" | NAME == " 11 hrs", "G1", ifelse(NAME == " 5 hrs" | NAME == " 6 hrs", "S", "G2")));');

  my $colorMap = "c(\"G1\" = \"#aed6f1\", \"S\" = \"#f9e79f\", \"G2\" = \"#a9dfbf\")";
  $profile->setColorVals($colorMap);

  my $plotTitle = $profile->getPlotTitle();
  $profile->setPlotTitle($plotTitle . " : Cell cycle phases" );

  return $self;
}

1;


# for PlasmoDB Apicoplast_ER Quant Proteomes
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::NonRatio::DS_32db942cc7;


use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues;
use EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthSourceIdNames;
use Data::Dumper;

# @Override
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $specs = $self->getSpecs();
  my $id = $self->getId();

  my @profileSets;
  foreach my $ps (@$specs) {
    my @profileSet = $self->makeProfileSets($ps->{query}, $ps->{abbrev}, $ps->{name});
    push @profileSets, @profileSet;
  }
  my $go = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot->new(@_);
  $go->setProfileSets(\@profileSets);
  $go->setYaxisLabel("Apicoplast Abundance");
  $go->setPartName("apico.er");
  $go->setPlotTitle("$id - ER vs. Apicoplast Abundance");
  $go->setXaxisLabel("ER Abundance");
  $go->setColors(["grey", "red"]);
  $go->addAdjustProfile('
profile.df.full$LEGEND[grepl("ALL", profile.df.full$PROFILE_FILE)] <- "All Genes"
profile.df.full$LEGEND[!grepl("ALL", profile.df.full$PROFILE_FILE)] <- unlist(lapply(strsplit(profile.df.full$PROFILE_FILE[!grepl("ALL", profile.df.full$PROFILE_FILE)], "-"),"[",2)) 
profile.df.full$PROFILE_FILE[grepl("Apico", profile.df.full$PROFILE_FILE)] <- "Apico"
profile.df.full$PROFILE_FILE[grepl("ER", profile.df.full$PROFILE_FILE)] <- "ER"
profile.df.full$ELEMENT_NAMES_NUMERIC <- NULL
profile.df.full$Group.1 <- NULL
profile.df.full$ELEMENT_ORDER <- NULL
profile.df.full <- profile.df.full %>% spread(PROFILE_FILE, VALUE)
profile.df.full$ELEMENT_NAMES <- NULL
names(profile.df.full)[names(profile.df.full) == "ER"] <- "ELEMENT_NAMES_NUMERIC"
names(profile.df.full)[names(profile.df.full) == "Apico"] <- "VALUE"
#profile.df.full$VALUE[profile.df.full$VALUE == 0] <- 0.0000001
profile.df.full$ELEMENT_NAMES_NUMERIC[profile.df.full$ELEMENT_NAMES_NUMERIC == 0] <- NA
profile.df.full$VALUE <- as.numeric(profile.df.full$VALUE)
profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(profile.df.full$ELEMENT_NAMES_NUMERIC)
profile.df.full$PROFILE_FILE = "Dummy"
profile.is.numeric <- TRUE
');
  $go->setRPostscript("
gp = gp + scale_x_log10() +
  scale_y_log10() 
");

  $self->setGraphObjects($go);

  return $self;
}

sub getSpecs {
  return [ {abbrev => "Apico",
            name => "Apicoplast Abundance",
            query => "SELECT ga.source_id,
                      CASE WHEN (nafe.value = 0 ) THEN 0.0000001
                      ELSE nafe.value END as value
		      FROM apidbtuning.geneattributes ga, results.nafeatureexpression nafe
                      , study.protocolappnode pan, study.studylink sl, study.study s
                      WHERE nafe.na_feature_id = ga.na_feature_id
                      AND pan.protocol_app_node_id = sl.protocol_app_node_id
                      AND nafe.protocol_app_node_id = sl.protocol_app_node_id
                      AND sl.study_id = s.study_id
                      AND s.NAME = 'Apicoplast and ER Proteomes'
                      AND pan.NAME LIKE 'Apicoplast%' ",
           },
           {abbrev => "ER",
            name => "ER Abundance",
            query => "SELECT ga.source_id,
                      CASE WHEN (nafe.value = 0 ) THEN 0.0000001
                      ELSE nafe.value END as value
                      FROM apidbtuning.geneattributes ga, results.nafeatureexpression nafe
                      , study.protocolappnode pan, study.studylink sl, study.study s
                      WHERE nafe.na_feature_id = ga.na_feature_id
                      AND pan.protocol_app_node_id = sl.protocol_app_node_id
                      AND nafe.protocol_app_node_id = sl.protocol_app_node_id
                      AND sl.study_id = s.study_id
                      AND s.NAME = 'Apicoplast and ER Proteomes'
                      AND pan.NAME LIKE 'ER%' ",

           },
      ];
}

sub makeProfileSets {
  my ($self, $sourceIdValueQuery, $abbrev, $name) = @_;

  my $id = $self->getId();

  my $goValuesCannedQueryCurve = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 800, Name => "_${abbrev}_av", Id => 'ALL');

  my $goNamesCannedQueryCurve = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthSourceIdNames->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 800, Name => "_${abbrev}_aen", Id => 'ALL');

  my $goValuesCannedQueryGene = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthValues->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 800, Name => "_${abbrev}_gv", Id => $id);

  my $goNamesCannedQueryGene = EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthSourceIdNames->new
      ( SourceIdValueQuery => $sourceIdValueQuery, N => 800, Name => "_${abbrev}_gen", Id => $id);

  my $goProfileSetCurve = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetCurve->setProfileCannedQuery($goValuesCannedQueryCurve);
  $goProfileSetCurve->setProfileNamesCannedQuery($goNamesCannedQueryCurve);

  my $goProfileSetGene = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetGene->setProfileCannedQuery($goValuesCannedQueryGene);
  $goProfileSetGene->setProfileNamesCannedQuery($goNamesCannedQueryGene);

  return(($goProfileSetCurve, $goProfileSetGene));
}

1;



#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR proteomicsSimpleNonRatio
