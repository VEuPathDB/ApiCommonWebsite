#!/bin/bash

webDir=/var/www/cryptodb.org
bgColor="#9ccfff"
project=CryptoDB
icon=/images/CmurisSEM1_color_tiny.gif

PC_SCRIPT=$webDir/Common/pubcrawler/pubcrawlerApi.pl
CONFIG=$webDir/Common/pubcrawler/pubcrawler.config
perl $PC_SCRIPT -mute -c $CONFIG -bg $bgColor -proj $project -icon $icon
