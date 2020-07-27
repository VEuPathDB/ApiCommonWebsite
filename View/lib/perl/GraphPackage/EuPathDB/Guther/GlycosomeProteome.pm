package ApiCommonWebsite::View::GraphPackage::EuPathDB::Guther::GlycosomeProteome;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;
use LWP::Simple;
use JSON;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $geneId = $self->getId();

  my %colors = ( '0' => ['red2'],     # 0: "unlikely to be glycosomal"
	       '1' => ['darkgray'], # 1: "possible glycosomal"
	       '2' => ['green2']    # 2: "high-confidence glycosomal"
	       );

  my $url = $self->getBaseUrl() . '/a/service/profileSet/GutherCategory/' . $geneId;
  my $content = get($url);
  my $json = from_json($content);
  my $colorNum = @$json[0]->{'CAT_VAL'};

  my @profileSetsArray = (['Procyclic stage glycosome proteome', 'values', ]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $quant = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::QuantMassSpec->new(@_);
  $quant->setProfileSets($profileSets);
  $quant->setColors( $colors{$colorNum});
  $quant->setForceHorizontalXAxis(0);
  $quant->setYaxisLabel('Log2(H/L)');

  my $sampleLabel = ["Confidence Group"];
  $quant->setSampleLabels($sampleLabel);

  $self->setGraphObjects($quant,);
}

1;
