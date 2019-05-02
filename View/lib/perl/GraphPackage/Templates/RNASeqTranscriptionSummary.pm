package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getAllProfileSetNames {
  my ($self) = @_;

  my $id = $self->getId();
  my $sql = "select profile_set_name from apidbtuning.profile 
			where dataset_type = 'transcript_expression' 
			and dataset_subtype = 'rnaseq' 
			and profile_type = 'values' 
			and source_id = '$id'
			and profile_set_name not like '%nonunique%'
			and (profile_set_name like '%firststrand%' 
				or profile_set_name like '%unstranded%')";

  my $dbh = $self->getQueryHandle();
  my $sh = $dbh->prepare($sql);
  $sh->execute();

  my @rv = ();
  while(my ($profileName) = $sh->fetchrow_array()) {
    next if($self->isExcludedProfileSet($profileName));
    my $p = {profileName=>$profileName, profileType=>'values'};
    push @rv, $p;
  }
  $sh->finish();

  return \@rv;
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
  sub getXAxisLabel { 'FPKM' }

1;
