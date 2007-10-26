the definitive version of the glossary is glossary.txt. 

the perl script ApiCommonData/Load/bin converts the .txt to wdk .xml

for now, this needs to be done manually when glossary.txt is edited.

if this proves to be a maintenance problem, we'll need to port the conversion to xsl and invoke it automatically from the wdk.

note: if the txt file is read back into excel, then when it returns, run dos2unix on it.
