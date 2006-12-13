#!/bin/bash

workingDir=/www/toxodb.org/Common/pubcrawler
bgColor="#cd919e"
project=ToxoDB
icon=/toxo/images/toxodb_logo.gif

PC_SCRIPT=$workingDir/pubcrawlerApi.pl
CONFIG=$workingDir/pubcrawler.config
perl $PC_SCRIPT -d $workingDir -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
