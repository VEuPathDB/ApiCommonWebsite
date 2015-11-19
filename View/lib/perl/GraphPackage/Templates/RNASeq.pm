package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

sub getKey{
  my ($self, $profileSetName, $profileType) = @_;
  my ($suffix) = $profileSetName =~ /\- (.+) \[/;
  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- /;
  $suffix = '_suffix' if (!$suffix);
  die if (!$strand);

  return "${suffix}_${profileType}_${strand}";
}

sub sortKeys {
  my ($a_suffix, $a_type, $a_strand) = split("\_", $a);
  my ($b_suffix, $b_type, $b_strand) = split("\_", $b);
  return ($b_type cmp $a_type)  && ($a_suffix cmp $b_suffix) && ($a_strand cmp $b_strand);

}


sub isExcludedProfileSet {
  my ($self, $psName) = @_;
  my $val =   $self->SUPER::isExcludedProfileSet($self, $psName);
  if ($val) {
#print STDERR "exclude by super - return 1\n";
    return 1;
  } elsif ($psName =~ /htseq-intersection/){
#print STDERR "exclude intersection: $psName - return 1\n";
    return 1;
  } else {
#print STDERR "$psName - return 0\n";
    return 0;
  }

}

1;


#--------------------------------------------------------------------------------
# TEMPLATE_ANCHOR rnaSeqGraph

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_66f9e70b8a;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use strict;
sub getGraphType { 'bar' }
sub excludedProfileSetsString { '' }
sub getSampleLabelsString {''}
sub getColorsString {}
sub getForceXLabelsHorizontalString { '1' } 
sub getBottomMarginSize {  }
sub getExprPlotPartModuleString { 'RNASeq' }
sub getXAxisLabel { '' }

1;
