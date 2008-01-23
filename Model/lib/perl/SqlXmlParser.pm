package ApiCommonWebsite::Model::SqlXmlParser;

use strict;
use Data::Dumper;
use XML::Simple;

# parse an sql dictionary in xml, in the form expected by the gbrowse DAS adaptor.
# in particular, it must be in this form:
#  <DONTCARE>
#    <module>
#      <sqlQuery>
#        <name>the_query_name</name>
#        <sql>
#         the sql here
#        </sql>
#      </sqlQuery>
#    </module>
#  </DONTCARE>

sub parseSqlXmlFile {
  my($sqlXmlFile, $showParse) = @_;

  open(FILE, $sqlXmlFile) || die "can't open sql xml file '$sqlXmlFile' for reading\n";
  my $simple = XML::Simple->new();

  # use forcearray so elements with one child are still arrays
  # and, use keyattr so that handlers are given as an ordered list
  # rather than a hash with name as key.  the ordering is needed
  # so that undo operations are ordered.  also, the qualifiers retain
  # the ordering found in the xml file.
  my $data = $simple->XMLin($sqlXmlFile,
			    forcearray => 1,
	      	    KeyAttr => {});
  if ($showParse) {
    print Dumper($data);
    print  "\n\n\n";
  }
  return $data;
}
1;
