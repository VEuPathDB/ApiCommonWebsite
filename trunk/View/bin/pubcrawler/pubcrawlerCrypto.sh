#!/bin/bash

workingDir=/var/www/cryptodb.org/Common/pubcrawler
bgColor="#FFCCCC"
project=CryptoDB
icon=/images/cryptodb_logo.gif

PC_SCRIPT=$workingDir/pubcrawlerApi.pl
CONFIG=$workingDir/pubcrawler.config
perl $PC_SCRIPT -d $workingDir -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
