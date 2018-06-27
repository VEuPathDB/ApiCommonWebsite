#!/bin/bash
#
# June 2018
# when getting/updating the file from svn, make sure it is executable (chmod uog+x) 
#
# you ll be asked to provide:
# - path to the directory where you have the project/files such as PlasmoDB/news.xml (...../ApiCommonWebsite/Model/lib/xml)
# - new release Build# (a record should exist)
# - current date field (eg: 21 Jun 2018). This could bethe previous release date -if you create a new record by copying the previous one- or if the release has been postponed, you might have the previously planned date
# - current tag (eg: 06_18, used in PlasmoDB06_18_release)
# - new release date (eg: 03 Jul 2018)
# - new tag (eg: 07_18)
#
#
clear

myAllProjects="AmoebaDB CryptoDB EuPathDB FungiDB GiardiaDB HostDB MicrosporidiaDB PiroplasmaDB PlasmoDB SchistoDB ToxoDB TrichDB TriTrypDB"

read -p 'Enter the new Build # (eg: 28): ' myBuild
let "myOldBuild = $myBuild -1"
echo We will be performing date/tag substitutions in all news.xml files for the headline: \'xxxxxxDB $myBuild Released\'.
echo If $myBuild is incorrect type Ctrl-C to stop the run
echo
echo If there is no record with this Build number, nothing will be done.
echo Backup files news.xml.bak will be created so if something goes wrong you can restore the original files
# for i in `ls`; do(echo $i; cp $i/news.xml.bak $i/news.xml;  echo ------- ;); done;.
echo
read -p 'Enter the path to the news files directory: ' myPath
echo The news.xml files are expected in: $myPath/xxxxxDB/news.xml
echo
read -p 'Enter old release date (eg: 01 Jan 2000): ' myOldDate
read -p 'Enter old tag (month_date: eg: 01_00): ' myOldTag
echo myOldDate = ${myOldDate}
echo myOldTag = ${myOldTag}
echo
read -p 'Enter new release date (eg: 01 Jan 2000): ' myDate
read -p 'Enter new tag (month_date: eg: 01_00): ' myTag
echo myNewDate = ${myDate}
echo myNewTag = ${myTag}
echo

for myProject in $myAllProjects
do

echo
myFile=${myPath}/${myProject}/news.xml

if [ -e $myFile ]
then
echo Updating $myFile, creating backup file .bak
echo
echo "Replacing first occurrence of date ${myOldDate} 13:00 with ${myDate} 13:00"
sed -i.bak -e "/${myProject} ${myBuild} Released/,/${myProject} ${myOldBuild} Released/  s/${myOldDate} 13:00/${myDate} 13:00/" $myFile
echo "Replacing first occurrence of tag ${myProject}${myOldTag}_release with ${myProject}${myTag}_release"
sed -ie "/${myProject} ${myBuild} Released/,/${myProject} ${myOldBuild} Released/   s/${myProject}${myOldTag}_release/${myProject}${myTag}_release/I" $myFile
echo DONE $myProject!
else
echo $myFile does not exist
fi

echo
echo =============

done





