package EbrcWebsiteCommon::View::GraphPackage::EuPathDB::OverlayIRBC::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

use EbrcWebsiteCommon::View::GraphPackage::EuPathDB::Winzeler::Mapping;
use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'red', 'orange', 'cyan', 'purple' );
  my @legend = ('HB3', '3D7', 'DD2', '3D7 Sorbitol', '3D7 Temperature');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});
  $self->setPlotWidth(450);

  my $_3d7ProfileSet = 'DeRisi 3D7 Smoothed';
  my $hb3ProfileSet = 'DeRisi HB3 Smoothed';
  my $dd2ProfileSet = 'DeRisi Dd2 Smoothed';

  my $times_3d7 = $self->getTimePointMapping($_3d7ProfileSet, '48 Hour Cycle Timepoint Map for 3D7');
  my $times_hb3 = $self->getTimePointMapping($hb3ProfileSet, '48 Hour Cycle Timepoint Map for HB3');
  my $times_dd2 = $self->getTimePointMapping($dd2ProfileSet, '48 Hour Cycle Timepoint Map for Dd2');

  my @temp_times = EbrcWebsiteCommon::View::GraphPackage::EuPathDB::Winzeler::Mapping::TemperatureTimes();
  my @sorb_times = EbrcWebsiteCommon::View::GraphPackage::EuPathDB::Winzeler::Mapping::SorbitolTimes();


  my @derisiProfileArray = ([$hb3ProfileSet, '', $times_hb3],
                            [$_3d7ProfileSet, '', $times_3d7],
                            [$dd2ProfileSet, '', $times_dd2],
                           );

  my $derisiProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@derisiProfileArray);

  my $derisi = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $derisi->setProfileSets($derisiProfileSets);
  $derisi->setColors([@colors[0..2]]);
  $derisi->setPointsPch([15,15,15]);
  $derisi->setPartName('derisi');
  

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','', \@sorb_times],
                              ['winzeler_cc_tempExp', '', \@temp_times]
                             );

  my $winzelerProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);

  my $winzeler = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors([@colors[3..4]]);
  $winzeler->setPartName('winzeler');
  $winzeler->setPointsPch([15,15]);
  $winzeler->setAdjustProfile('lines.df = lines.df - mean(lines.df[lines.df > 0], na.rm=T)');


  my $percentileProfileArray = [['red percentile - DeRisi HB3 Smoothed', '', $times_hb3],
                                ['red percentile - DeRisi 3D7 Smoothed', '', $times_3d7],
                                ['red percentile - DeRisi Dd2 Smoothed', '', $times_dd2],
                                ['percentile - winzeler_cc_sorbExp', '', \@sorb_times],
                                ['percentile - winzeler_cc_tempExp', '', \@temp_times]
                                ];

  my $percentileProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets($percentileProfileArray);

   my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
   $percentile->setProfileSets($percentileProfileSets);
   $percentile->setColors(\@colors);
   $percentile->setPointsPch(['NA','NA','NA','NA','NA']);

  $self->setGraphObjects($derisi, $winzeler, $percentile);

  return $self;
}



sub getTimePointMapping {
  my ($self, $profileSetName, $timePointProfileSetName) = @_;

  my $sql = "select pen.name, tpp.profile_as_string
from apidb.profileset ps, apidb.profileelementname pen,
     apidb.profile tpp, apidb.profileset tpps
where pen.profile_set_id = ps.profile_set_id
and ps.name = ?
and tpps.profile_set_id = tpp.profile_set_id
and tpps.name = ?
and tpp.source_id = pen.name
order by pen.element_order";

  my $qh = $self->getQueryHandle();

  my $sh = $qh->prepare($sql);

  $sh->execute($profileSetName, $timePointProfileSetName);

  my @rv;

  while(my ($old, $new) = $sh->fetchrow_array()) {
    push @rv, $new;
  }
  $sh->finish();

  return \@rv;
}

1;
