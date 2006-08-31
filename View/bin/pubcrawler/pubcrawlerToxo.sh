#!/bin/bash

webDir=/www/toxoodb.org
bgColor="#2ccfff"  
project=ToxoDB
icon=images/toxodb_logo.gif

PC_SCRIPT=$webDir/Common/pubcrawler/pubcrawlerApi.pl
CONFIG=$webDir/Common/pubcrawler/pubcrawler.config
perl $PC_SCRIPT -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
