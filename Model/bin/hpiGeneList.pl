#!/usr/bin/perl

use lib "$ENV{GUS_HOME}/lib/perl";

use ApiCommonWebsite::Model::HpiGeneList;

use strict;

my $hpiGeneList = ApiCommonWebsite::Model::HpiGeneList->new();


$hpiGeneList->usage() unless scalar(@ARGV) == 10;

$hpiGeneList->run(@ARGV);

1;

