package ApiCommonWebsite::View::GraphPackage::AbstractPlotSet;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );
use ApiCommonWebsite::View::GraphPackage;

use ApiCommonWebsite::Model::CannedQuery::ElementNames;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileFixedValue;
use ApiCommonWebsite::View::MultiScreen;
use ApiCommonWebsite::View::GraphPackage::Util;

#--------------------------------------------------------------------------------

#TODO: would like to factor out into PlotPart.pm
sub getScreenSize                { $_[0]->{'_screen_size'                 }}
sub setScreenSize                { $_[0]->{'_screen_size'                 } = $_[1]; $_[0] }

#TODO: this needs to be factored into PlotPart
sub getGraphDefaultValue         { $_[0]->{'_graph_default_value'         }}
sub setGraphDefaultValue         { $_[0]->{'_graph_default_value'         } = $_[1]; $_[0] }

#TODO: needs would like to factor into PlotPart
sub getBottomMarginSize          { $_[0]->{'_bottom_margin_size'          }}
sub setBottomMarginSize          { $_[0]->{'_bottom_margin_size'          } = $_[1]; $_[0] }

sub getProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } }
sub setProfileSetsHash           { $_[0]->{'_profile_sets_hash'           } = $_[1]; $_[0] }

sub getMultiScreen               { $_[0]->{'_multi_screen'                } }
sub setMultiScreen               { $_[0]->{'_multi_screen'                } = $_[1]; $_[0] }

sub getFileHandle                { $_[0]->{'_file_handle'                 } }
sub setFileHandle                { $_[0]->{'_file_handle'                 } = $_[1]; $_[0] }

sub getPlotWidth                 { $_[0]->{'_plot_width'                  } }
sub setPlotWidth                 { $_[0]->{'_plot_width'                  } = $_[1]; $_[0] }

sub getMainLegend                { $_[0]->{'_main_legend'                 }}
sub setMainLegend                { $_[0]->{'_main_legend'                 } = $_[1]; $_[0] }

sub getLegendSize                { $_[0]->{'_legend_size'                 }}
sub setLegendSize                { $_[0]->{'_legend_size'                 } = $_[1]; $_[0] }

sub getAllNames                  { $_[0]->{'_all_names'                   }}
sub setAllNames                  { $_[0]->{'_all_names'                   } = $_[1]; $_[0] }

sub getAllValues                 { $_[0]->{'_all_values'                  }}
sub setAllValues                 { $_[0]->{'_all_values'                  } = $_[1]; $_[0] }

sub getTempFiles                 { $_[0]->{'_temp_files'                  }}
sub setTempFiles                 { $_[0]->{'_temp_files'                  } = $_[1]; $_[0] }
sub addTempFile {
  my ($self, $file) = @_;

  push @{$self->getTempFiles()}, $file;
}

#--------------------------------------------------------------------------------
# Abstract methods
#--------------------------------------------------------------------------------

sub makeRPlotStrings {}

#--------------------------------------------------------------------------------

sub init {
  my ($self) = @_;

  $self->SUPER::init(@_);

  my $r_f = $self->getOutputFile(). '.R';
  my $r_fh = FileHandle->new(">$r_f") || die "Can not open R file '$r_f': $!";

  $self->setFileHandle($r_fh);

  # Default 
  $self->setPlotWidth(425);
  $self->setLegendSize(40);

  $self->setTempFiles([]);

  $self->setProfileSetsHash([]);

  $self->setAllNames([]);
  $self->setAllValues({});

  $self;
}

#--------------------------------------------------------------------------------
#TODO: needs to be factored out into PlotPart.pm/ Also needs to be added to BarPlot/LinePlot 
sub hasGraphDefault {
  my ($self) = @_;

  if(defined($self->getGraphDefaultValue())) {
    return 1;
  }
  return 0;
}

#--------------------------------------------------------------------------------

sub makeRLegendString {
  my ($self) = @_;

  my $legendHash = $self->getMainLegend();

  unless($legendHash) {
    return "  screen(screens[screen.i]);
  screen.i <- screen.i + 1;";
  }

  my $colors = $legendHash->{colors};
  my $names = $legendHash->{short_names};
  my $pch = $legendHash->{points_pch};
  my $fill = $legendHash->{fill};
  my $nCols = $legendHash->{cols};

  my $rColorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'legend.colors');
  my $rNamesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($names, 'legend.names');
  my $rPointsPchString = ApiCommonWebsite::View::GraphPackage::Util::rNumericVectorFromArray($pch, 'points.pch');
  my $rFill = $fill ? "TRUE" : "FALSE";

  $nCols = defined($nCols) ? $nCols : 2;

  my $rv = "
 #-------------------------------------------------------------------------------
  screen(screens[screen.i]);
  screen.i <- screen.i + 1;

  $rColorsString
  $rNamesString
  $rPointsPchString

  par(yaxs='i', xaxs='i', xaxt='n', yaxt='n', bty='n', mar=c(0.1,0.1,0.1,0.1));
  plot(c(0),c(0), xlab='', ylab='',type='l',col='orange', xlim=c(0,1),ylim=c(0,1));

  if($rFill) {
    legend(0.5, 0.5,
         legend.names,
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.9,
         ncol  = $nCols,
         fill=legend.colors,
         bty='n'
        );
  } else {
    legend(0.5, 0.5,
         legend.names,
         xjust = 0.5,
         yjust = 0.5,
         cex   = 0.9,
         pt.cex = 1.5,
         col   = legend.colors,
         pt.bg = legend.colors,
         pch   = points.pch,
         lty   = 'solid',
         ncol  = $nCols,
         bty='n'
        );
  }
";


}

#--------------------------------------------------------------------------------

sub makeR {
  my ($self, $rPlotStringsHash) = @_;

  my @rv;

  my $thumb_b   = $self->getThumbnail();

  my $r_f = $self->getOutputFile(). '.R';
  my $r_fh = $self->getFileHandle();
  my $out_f     = $self->getOutputFile();

  push(@rv, $r_f, $out_f);

  my $parts = [];

  my $legendSize = 1;
  if($self->getMainLegend()) {
    $legendSize = $self->getLegendSize();
  }

  push(@$parts, { Name => "_LEGEND",   Size => $legendSize });


  my $profileSetsHash = $self->getProfileSetsHash();

  foreach my $ps (keys %$profileSetsHash) {
    my $sizeFromHash = $profileSetsHash->{$ps}->{size};
    my $size = defined $sizeFromHash ? $sizeFromHash : $self->getScreenSize();
    push(@$parts, { Name => "$ps",   Size => $size});
  }

  my $mS = ApiCommonWebsite::View::MultiScreen->new
    ( Parts => $parts,
      VisibleParts => $self->getVisibleParts(),
      Thumbnail    => $thumb_b
    );

  $self->setMultiScreen($mS);

  my $width       = $self->getPlotWidth();
  my $totalHeight = $mS->totalHeight();
  my $scale       = $self->getScalingFactor();

  $width       *= $scale;
  $totalHeight *= $scale;

  # used in R code to set locations of screens
  my $screens     = $mS->rScreenVectors();
  my $parts_n     = $mS->numberOfVisibleParts();

  my $open_R;

  my $widthOverride = $self->getWidthOverride();
  my $heightOverride = $self->getHeightOverride();


  if($widthOverride && $heightOverride) {
    $open_R = $self->rOpenFile($widthOverride, $heightOverride);
  }
  else {
    $open_R = $self->rOpenFile($width, $totalHeight);
  }

  my $preamble_R  = $self->_rStandardComponents($thumb_b);

  my     $legend = "";

  my %isVis_b = $mS->partIsVisible();

  # Always want _LEGEND available to visible parts
  if($isVis_b{_LEGEND}) {
    $legend = $self->makeRLegendString();
  }

  my @rStrings = @{$self->makeRPlotStrings()};
  my $rStrings = join("\n", @rStrings);

  my $rcode =  <<RCODE;

# ------------------------------- Prepare --------------------------------

$preamble_R

$open_R;

plasmodb.par();

screen.dims <- t(array(c($screens),dim=c(4,$parts_n)));
screens     <- split.screen(screen.dims, erase=T);
screens;
screen.i    <- 1;

ticks <- function() {
  axis(1, at=seq(x.min, x.max, 1), labels=F, col="gray75");
  axis(1, at=seq(5*floor(x.min/5+0.5), x.max, 5), labels=F, col="gray50");
  axis(1);
}

# --------------------------------- Add Legend-------------------------------

$legend

# --------------------------------- Add Plots ------------------------------

$rStrings


# --------------------------------- Done ---------------------------------

close.screen(all.screens=T);
dev.off();
quit(save="no")

RCODE


  print $r_fh $rcode;
#  print STDERR $rcode;

  $r_fh->close();

  my $tempFiles = $self->getTempFiles();

  push @rv, @$tempFiles;


  return @rv;
}

#--------------------------------------------------------------------------------

#TODO: factor out into PlotPart.pm
sub writeProfileFiles {
  my ($self, $profileSetName, $suffix, $elementOrder) = @_;

  my $_qh   = $self->getQueryHandle();
  my $_dict = {};

  my $r_fh = $self->getFileHandle();

  my $defaultProfile;

  if($self->hasGraphDefault()) {
    my $defaultValue = $self->getGraphDefaultValue();

    $defaultProfile = ApiCommonWebsite::Model::CannedQuery::ProfileFixedValue->new
      ( Name         => "_data_$suffix",
        Id           => $self->getId(),
        ProfileSet   => $profileSetName,
        DefaultValue => $defaultValue,
      );
  }


  my $profile = ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => "_data_$suffix",
      Id           => $self->getId(),
      ProfileSet   => $profileSetName,
    );

  my $elementNames = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_names_$suffix",
        Id           => $self->getId(),
        ProfileSet   => $profileSetName,
      );

  my @profileErrors;
  my @errors;

  $profile->setElementOrder($elementOrder) if($elementOrder);
  $elementNames->setElementOrder($elementOrder) if($elementOrder);
  my $profile_fn = eval { $profile->makeTabFile($_qh, $_dict) }; $@ && push(@profileErrors, $@);
  my $elementNames_fn = eval { $elementNames->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
#TODO: factor into PlotPart, each PlotPart must be responsible for deleting temp files 
  $self->addTempFile($profile_fn) if($profile_fn);
  $self->addTempFile($elementNames_fn) if($elementNames_fn);

  if(@profileErrors) {
    $profile_fn = eval { $defaultProfile->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
#TODO: factor into PlotPart, each PlotPart must be responsible for deleting temp files 
    $self->addTempFile($profile_fn) if($profile_fn);
  }

  my @rv = ($profile_fn, $elementNames_fn);

  if (@errors) {
    $self->reportErrorsAndBlankGraph($r_fh, (@profileErrors,@errors));
  }

  return \@rv;
}


#--------------------------------------------------------------------------------

sub addToProfileDataMatrix {
  my ($self, $profileFiles, $elementNamesFiles, $profileSetNames) = @_;

  my $allNames = $self->getAllNames();
  my $allValues = $self->getAllValues();

  for(my $i = 0; $i < scalar @$profileFiles; $i++) {
    my $profileFile = $profileFiles->[$i];
    my $elementNamesFile = $elementNamesFiles->[$i];
    my $profileSet = $profileSetNames->[$i];
    
    next unless ($profileFile && $elementNamesFile);

    open(NAMES, $elementNamesFile) or die "Cannot open file $elementNamesFile for reading:$!";
    open(VALUES, $profileFile) or die "Cannot open file $elementNamesFile for reading:$!";

    my @names = <NAMES>;
    my @values = <VALUES>;

    my %values;
    my $index = 1;

    foreach(@values) {
      chomp $_;
      next if /VALUE/;
      my ($k, $v) = split(/\t/, $_);

      if(defined $v) {
        push @{$values{$k}}, $v;
      } else {
        push @{$values{$index}}, $k;
        $index++;
      }
    }


    my @avgValues = ('Header');

    for(my $i = 1; $i < scalar(keys(%values)) + 1; $i++) {
      my @allValues = @{$values{$i}};

      my $sum = 0;
      my $naCount = 0;
      foreach(@allValues) {
        if($_ eq 'NA') {
          $naCount++;
        }
        else {
          $sum += $_;
        }
      }

      if($naCount == scalar @allValues) {
        push @avgValues, 'NA';
      }
      else {
        push @avgValues, $sum / (scalar(@allValues) - $naCount);
      }
    }

    unless(scalar @names == scalar @avgValues) {
      die "Element Names file Different length than Values File";
    }

    for(my $j = 1; $j < scalar @names; $j++) {
      my $nameRow = $names[$j];

      chomp $nameRow;

      my ($neo, $name) = split(/\t/, $nameRow);
      my $value = $avgValues[$j];

      my ($digit) = $name =~ /(^\d+)/;
      $digit = -1 unless(defined $digit);

      my $namesHash = {name => $name,
                       digit => $digit,
                       elementOrder => $neo,
                      };

      push @$allNames, $namesHash unless(ApiCommonWebsite::View::GraphPackage::Util::isSeen($name, $allNames));

      my $distinctProfileSet = $i > 0 ? $profileSet . "_" . $i : $profileSet;

      $allValues->{$distinctProfileSet}->{$name} = $value;
    }


    close NAMES;
    close VALUES;
  }

}

#--------------------------------------------------------------------------------

sub makeHtmlStringFromMatrix {
  my ($self) = @_;

  my $allNames = $self->getAllNames();
  my $allValues = $self->getAllValues();

  my $outputFile = $self->getOutputFile();
  open(OUT, ">> $outputFile") or die "Cannot open file $outputFile for writing: $!";

  my @sortedNames = map { $_->{name} } sort{$a->{digit} <=> $b->{digit} || $a->{elementOrder} <=> $b->{elementOrder}} @$allNames;

  print OUT "<table border=1>\n  <thead><tr><th>Experiment/Sample</th>\n";

  my @values;

  my @profileSets = sort keys %$allValues;

    print OUT join("\n", map{ "    <th>$_</th>"} @profileSets);
    print OUT "  </tr></thead>\n";

  foreach my $elementName (@sortedNames) {
      print OUT "  <tr>\n";
      print OUT "  <td>$elementName</td>\n";

    foreach my $profileSet (@profileSets) {
      my $val = $allValues->{$profileSet}->{$elementName};

      $val = defined $val && $val ne 'NA' ? sprintf("%.3f", $val) : "NA";

      print OUT "  <td>$val</td>\n";
    }
      print OUT "  </tr>\n";
  }


  print OUT "</table>\n<br/><br/>";

  close OUT;
}




#--------------------------------------------------------------------------------





1;
