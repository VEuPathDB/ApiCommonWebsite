This README pertains to the glossary files glossary.txt and glossary.xml

- glossary.txt:  the definitive version of the glossary. you may edit this one.
- glossary.xml:  generated from glossary.txt.  you may not edit this one.


to update the glossary:
  - edit glossary.txt in ApiCommonWebsite/Model/lib/xml/
  - build the site, the script will be run and the xml file updated and copied into gus_home/lib/xml/


+++++++++++++++++++++++++++++++++++++++++++++++
OLD VERSION
to update the glossary:
  - edit glossary.txt
  - run glossaryTextToXml to generate glossary.xml (ApiCommonData/Load/bin)
  - now you have a wdk-compatible xml record file.  do the usual build, wdkXmlQuestion, jsp thing
  - when satisfied check both the .txt and .xml into svn

if this proves to be a maintenance problem, we'll need to port the generator to xsl and invoke it automatically from the wdk. 
+++++++++++++++++++++++++++++++++++++++++++++++


Notes
 - glossary.txt was orginally created by manually migrating Yolanda's excel version into text format, with some clean up along the way.
- if for any reason going back into excel (and then returning to text) is required, be sure to use dos2unix (or similar) to correct DOS newlines

