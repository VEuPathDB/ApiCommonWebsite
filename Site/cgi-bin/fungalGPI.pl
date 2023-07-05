#!/usr/bin/perl                                                                                                                    

use strict;

use lib "@targetDir@/lib/perl";
$ENV{GUS_HOME} = '@targetDir@';


use ApiCommonWebsite::View::CgiApp::FungalGPI;

my $app =
    ApiCommonWebsite::View::CgiApp::FungalGPI->new();

$app->go();

