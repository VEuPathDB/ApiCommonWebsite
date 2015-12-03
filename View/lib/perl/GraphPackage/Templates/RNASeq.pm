package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getKey{
  my ($self, $profileSetName, $profileType) = @_;

  my ($groupName) = $self->getGroupNameFromProfileSetName($profileSetName);
  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- /;
  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  die if (!$strand);
  $strand = $strand eq 'unstranded'? ''  :  '_' . $self->getStrandDictionary()->{$strand};
  return "${groupName}_${profileType}${strand}";
}

sub switchStrands {
  return 0;
}

# @Override
sub getGroupRegex {
  return qr/\- (.+) \[/;
}

# @Override
sub getRemainderRegex {
  return qr/\[(\S+) /;
}


sub getStrandDictionary {
  my $self = shift;
  my $firststrand = 'sense';
  my $secondstrand = 'antisense';

  if ($self->switchStrands()) {
    $firststrand = 'antisense';
    $secondstrand = 'sense';
  }
  return { 'unstranded' => '',
	   'firststrand' => $firststrand,
	   'secondstrand' => $secondstrand
	 };
}

# @Override
sub sortKeys {
  my ($a_suffix, $a_type, $a_strand) = split("\_", $a);
  my ($b_suffix, $b_type, $b_strand) = split("\_", $b);
  return ($b_type cmp $a_type)  && ($a_suffix cmp $b_suffix) && ($a_strand cmp $b_strand);

}

# @Override
sub isExcludedProfileSet {
  my ($self, $psName) = @_;
  my ($strand) = $psName =~ /\[.+ \- (.+) \- /;
  $strand = $self->getStrandDictionary()->{$strand};

  my ($isCufflinks) = ($psName =~/\[cuff \-/)? 1: 0;

  my $val =   $self->SUPER::isExcludedProfileSet($self, $psName);
  if ($val) {
#print STDERR "exclude by super - return 1\n";
    return 1;
  } elsif ($psName =~ /htseq-intersection/){
#print STDERR "exclude intersection: $psName - return 1\n";
    return 1;
  } elsif ($isCufflinks && $strand eq 'antisense'){
    return 1;
  } else {
#print STDERR "$psName - return 0\n";
    return 0;
  }

}

1;


#--------------------------------------------------------------------------------
# TEMPLATE_ANCHOR rnaSeqGraph

# package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_66f9e70b8a;
# use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
# use strict;
# sub getGraphType { 'bar' }
# sub excludedProfileSetsString { '' }
# sub getSampleLabelsString {''}
# sub getColorsString {}
# sub getForceXLabelsHorizontalString { '1' } 
# sub getBottomMarginSize {  }
# sub getExprPlotPartModuleString { 'RNASeq' }
# sub getXAxisLabel { '' }

1;
