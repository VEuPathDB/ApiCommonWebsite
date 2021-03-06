package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandNonSpecific;

use strict;

use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;

use EbrcWebsiteCommon::View::GraphPackage::Util;
use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeqLinePlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

use Data::Dumper;

sub getColor { $_[0]->{_color} }
sub setColor { $_[0]->{_color} = $_[1] }

sub getIsPairedEnd { $_[0]->{_is_paired_end} }
sub setIsPairedEnd { $_[0]->{_is_paired_end} = $_[1] }

sub getForceXLabelsHorizontalString {$_[0]->{_force_x_labels_horizontal}}
sub setForceXLabelsHorizontalString {$_[0]->{_force_x_labels_horizontal} = $_[1]}

sub getBottomMarginSize { $_[0]->{_bottom_margin_size} }
sub setBottomMarginSize { $_[0]->{_bottom_margin_size} = $_[1] }


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $datasetName = $self->getDataset();

  my $id = $self->getId();

  my $dbh = $self->getQueryHandle();

  my $sql = EbrcWebsiteCommon::View::GraphPackage::Util::getProfileSetsSql();

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);

  my %hash;

  while(my ($profileName) = $sh->fetchrow_array()) {
    my $key = 'DEFAULT';

    if($profileName =~ /-\s?diff$/) {
      $key = $1 if($profileName =~ /-\s+(\w+)\s+-\s?diff$/);
      $hash{$key}->{diff} = $profileName;
    } elsif($profileName =~ /^percentile\s?-/) {
      $key = $1  if($profileName =~ /-\s+(\w+)$/);
      $hash{$key}->{percentile} = $profileName;
    } else {
      $key = $1  if($profileName =~ /-\s+(\w+)$/);
      $hash{$key}->{main} = $profileName;
    }

  }
  $sh->finish();

  my @rnaseqs;

  my $visibleParts = $self->getVisibleParts();
  my @newVisibleParts;

  foreach my $key (keys %hash) {
    my $count = scalar keys %{$hash{$key}};

    die "Expected 3 rows in profileset for $datasetName and profile $key" if($count != 3);

    my $rnaseq = EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq->new(@_);

    $rnaseq->setMinRpkmProfileSet($hash{$key}->{main});
    $rnaseq->setDiffRpkmProfileSet($hash{$key}->{diff});
    $rnaseq->setPctProfileSet($hash{$key}->{percentile});

    $rnaseq->setColor($self->getColor());
    $rnaseq->setIsPairedEnd($self->getIsPairedEnd());
    $rnaseq->setBottomMarginSize($self->getBottomMarginSize());
    $rnaseq->setForceXLabelsHorizontalString($self->getForceXLabelsHorizontalString());
    $rnaseq->setSampleLabelsString($self->getSampleLabelsString());

    $rnaseq->makeGraphs(@_);

    my ($rnaseqStacked, $rnaseqPct) = @{$rnaseq->getGraphObjects()};

    if($key ne 'DEFAULT') {
      foreach my $part (@$visibleParts) {
        push @newVisibleParts,  "${key}_" . $rnaseqStacked->getPartName if($rnaseqStacked->getPartName eq $part);
        push @newVisibleParts,  "${key}_" . $rnaseqPct->getPartName if($rnaseqPct->getPartName eq $part);
      }
      $rnaseqStacked->setPartName("${key}_" . $rnaseqStacked->getPartName);
      $rnaseqPct->setPartName("${key}_" . $rnaseqPct->getPartName);
      $rnaseqStacked->setPlotTitle($rnaseqStacked->getPlotTitle() . " - $key");
      $rnaseqPct->setPlotTitle($rnaseqPct->getPlotTitle() . " - $key");
    }

    push @rnaseqs, $rnaseqStacked, $rnaseqPct;
  }

  push @$visibleParts, @newVisibleParts;
  $self->setVisibleParts($visibleParts);

  $self->setGraphObjects(@rnaseqs);

  return $self;
}

1;


#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandNonSpecific::pberANKA_Waters_Stunnenberg_five_stages_rnaSeq_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandNonSpecific );
use strict;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  foreach my $plotPart (@{$self->{_graph_objects}}) {
    my $profileSets = $plotPart->getProfileSets();
    foreach my $profileSet (@$profileSets) {
      if($profileSet->getName() =~ /cl15cy1/) {
        $profileSet->setElementNames(["4H R", "16H T", "22H S", "G", "O"]);
      }
      if($profileSet->getName() =~ /1037cl1/) {
        $profileSet->setElementNames(["4H R", "16H T", "22H S", "G"]);
      }

    }

    
  }


  return $self;
}

#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR rnaSeqStrandNonSpecificGraph
