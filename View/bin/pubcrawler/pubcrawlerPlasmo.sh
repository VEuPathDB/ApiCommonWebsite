#!/bin/bash

webDir=/www/plasmodb.org
bgColor="#bbaacc"
project=PlasmoDB
icon=/plasmo/images/plasmodb_logo.gif

PC_SCRIPT=$webDir/Common/pubcrawler/pubcrawlerApi.pl
CONFIG=$webDir/Common/pubcrawler/pubcrawler.config
perl $PC_SCRIPT -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
