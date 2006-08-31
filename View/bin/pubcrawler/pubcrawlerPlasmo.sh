#!/bin/bash

webDir=/www/plasmodb.org
bgColor="#9ccfff"
project=PlasmoDB
icon=images/plasmodb_logo.gif

PC_SCRIPT=$webDir/PubCrawler/pubcrawlerApi.pl
CONFIG=$webDir/PubCrawler/pubcrawler.config
perl $PC_SCRIPT -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
