#!/bin/bash

workingDir=/www/plasmodb.org/Common/pubcrawler
bgColor="#bbaacc"
project=PlasmoDB
icon=/plasmo/images/plasmodb_logo.gif

PC_SCRIPT=$workingDir/pubcrawlerApi.pl
CONFIG=$workingDir/pubcrawler.config
perl $PC_SCRIPT -d $workingDir -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
