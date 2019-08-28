package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getAllProfileSetNames {
  my ($self) = @_;

  my @unionStr = ( 
			#TEMPLATE_ANCHOR transcriptionSummaryGraph
		);
  my $unionStr = join "\n", @unionStr;

  my $id = $self->getId();
  my $sql = "select distinct profile_set_name,
                    dsp.display_name || decode(p.profile_set_suffix, null, '', ' - ' || p.profile_set_suffix) || ', ' || dsp.short_attribution as display_name,
                    dsp.dataset_presenter_id as presenter_id, 
		    ds.order_num
                from apidbtuning.profile p, 
                     apidbtuning.profilesamples ps, 
                     apidbtuning.expressiongraphsdata d,
                     apidbtuning.datasetpresenter dsp,
                    (select '' as dataset_name, 1 as order_num from dual
	$unionStr
                    ) ds
                where p.dataset_type = 'transcript_expression' 
                and p.dataset_subtype = 'rnaseq' 
                and p.profile_type = 'values' 
                and p.source_id = '$id'
                and d.sample_name not like '%antisense%'
                and d.sample_name like '%unique%'
                and p.profile_set_name = ps.study_name
                and ps.protocol_app_node_id = d.protocol_app_node_id
                and p.dataset_name = dsp.name
                and dsp.name = ds.dataset_name
                order by ds.order_num";

  my $dbh = $self->getQueryHandle();
  my $sh = $dbh->prepare($sql);
  $sh->execute();

  my @rv = ();
  while(my ($profileName, $displayName) = $sh->fetchrow_array()) {
    next if($self->isExcludedProfileSet($profileName));
    my $p = {profileName=>$profileName, profileType=>'values', displayName=>$displayName};
    push @rv, $p;
  }
  $sh->finish();

  return \@rv;
}

# @Override
# so as not to sort plotprofiles
sub makeAndSetPlots {
  my ($self, $plotParts, $hasStdError) = @_;
  my @rv;
  
  my $bottomMarginSize = $self->getBottomMarginSize();
  my $colors= $self->getProfileColors();
  my $pctColors= $self->getPercentileColors();
  my $sampleLabels = $self->getSampleLabels();

  foreach my $key (sort {$self->sortKeys($a, $b)} keys %$plotParts) {
    my @plotProfiles =  @{$plotParts->{$key} };
    my @profileSetsArray;

    foreach my $p (@plotProfiles) {
      if ($hasStdError->{ $p->{profileName}} && !($key=~/percentile/)) {
	push @profileSetsArray, [$p->{profileName}, $p->{profileType}, $p->{profileName}, 'standard_error'];
      } else {
	if (defined $p->{displayName}) {
	  push @profileSetsArray, [$p->{profileName}, $p->{profileType}, '','','','','','', $p->{displayName}];
	} else {
	  push @profileSetsArray, [$p->{profileName}, $p->{profileType}];
	}
      }
    }

    my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

    my $xAxisLabel;
    my $plotObj;
    my $plotPartModule = $key=~/percentile/? 'Percentile': $self->getExprPlotPartModuleString();
    
    if((lc($self->getGraphType()) eq 'bar' || ($key=~/percentile/ && blessed($self) =~/TwoChannel/)) && $self->useLegacy() ) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::BarPlot::$plotPartModule";
    } elsif($key=~/Both_strands/ && $plotPartModule eq 'RNASeq') {
	$self->setWantLogged(1);
	if(lc($self->getGraphType()) eq 'bar') {
	    $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::${plotPartModule}SenseAntisense";
	} elsif(lc($self->getGraphType()) eq 'line') {
	    $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::${plotPartModule}SenseAntisense";
	    $xAxisLabel= $self->getXAxisLabel();
	}	    
    } elsif((lc($self->getGraphType()) eq 'bar' || ($key=~/percentile/ && blessed($self) =~/TwoChannel/)) && !$self->useLegacy() ) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::$plotPartModule";
    } elsif(lc($self->getGraphType()) eq 'line' && $self->useLegacy()) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::LinePlot::$plotPartModule";
      $xAxisLabel= $self->getXAxisLabel();
    } elsif(lc($self->getGraphType()) eq 'line' && !$self->useLegacy()) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::$plotPartModule";
      $xAxisLabel= $self->getXAxisLabel();
    } elsif(lc($self->getGraphType()) eq 'scatter') {
      # TODO: handle two channel graphs in a different module
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio";
      $xAxisLabel= $self->getXAxisLabel();
    } else {
      die "Graph must define a graph type of bar or line";
    }

    my $profile = eval {
      $plotObj->new($self,$profileSets);
    };

    if ($@) {
      die "Unable to make plot $plotObj: $@";
    }

    if ($key!~/Both_strands/) {
       $profile->setProfileSets($profileSets);
       my @legendNames = map { $self->getRemainderNameFromProfileSetName($_->[0]) } @profileSetsArray;
       # omit the legend when there is just one profile, and it is not a RNASeq dataset
       my $keepSingleLegend = $self->keepSingleLegend();
       if  ($#legendNames || $keepSingleLegend) {
          $profile->setHasExtraLegend(1); 
          $profile->setLegendLabels(\@legendNames);
       }
    }

    my $profile_part_name = $profile->getPartName(); # percentile / rma
    $key =~s/values/$profile_part_name/;
    $key =~s/^\_//;
    $profile->setPartName($key);
    $profile->setPlotTitle("$key - " . $profile->getId() );
    my @profileTypes = map { $_->[1] } @profileSetsArray;
    $profile->setProfileTypes(\@profileTypes);

    if(lc($self->getGraphType()) eq 'bar') {
      $profile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());
    }

    if($bottomMarginSize) {
      $profile->setElementNameMarginSize($bottomMarginSize);
    }

    if($xAxisLabel) {
      $profile->setXaxisLabel($xAxisLabel);
    }

    if(@$sampleLabels) {
      $profile->setSampleLabels($sampleLabels);
    }

    # These can be implemented by the subclass if needed
    if ($key=~/percentile/) {
      $profile->setColors($pctColors);
    } 
    elsif ($key=~/Both_strands/) {
	my @colorArray = reverse(@{$colors});
	if (scalar @colorArray == 1) {
	    push @colorArray, "gray";
	}
	$profile->setColors(\@colorArray);
    }
    else {
      $profile->setColors($colors);
    }
    $self->finalProfileAdjustments($profile);
    push @rv, $profile;
  }
  $self->setGraphObjects(@rv);
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary::All;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary );

use strict;

  sub getGraphType { 'line' }
  sub excludedProfileSetsString { '' }
  sub getSampleLabelsString { '' }
  sub getColorsString { 'black' }
  sub getForceXLabelsHorizontalString { 'true' }
  sub getBottomMarginSize { 0 }
  sub getExprPlotPartModuleString { 'RNASeqTranscriptionSummary' }
  sub getXAxisLabel { 'FPKM - Sample 1' }

1;
