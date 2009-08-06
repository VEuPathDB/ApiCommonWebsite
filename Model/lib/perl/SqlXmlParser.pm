package ApiCommonWebsite::Model::SqlXmlParser;

use strict;
use XML::Simple;
use Data::Dumper;

sub new {
  my ($class, $file, $project, $showParse) = @_;
  my $self = {};

  die '\nmust supply $project arg to SqlXmlParser\n' unless $project;
  $self->{project} = $project;

  my $xsl = XML::Simple->new();

  # configure XMLin to give us a uniform structure of arrays of hashes
  my $fullTree = $xsl->XMLin($file,
			     Cache => 'memshare',
			     forcearray => 1, # singletons get an array
			     keyattr => {})   # no hashes of hashes
    or die "\ncannot open the sql file\n";

  print Dumper $fullTree if ($showParse);

  &pruneTree($self, $fullTree, $project);

  bless( $self, $class );
  return $self;
}

# post-process structure from XML simple to prune away excluded sqlQueries
# and produce structures we need
#  (1) hash of modules and sqlQueries for use by gbrowse
#  (2) array of sqlQueries for the sanity test
#  (3) hash of sanityTestInfo for sanity test
sub pruneTree {
  my ($self, $fullTree, $project) = @_;

  $self->{moduleHash} = {};
  $self->{sqlQueryArray} = [];
  $self->{sanityTestInfo} = $fullTree->{sanityTestInfo};

  foreach my $module (@{$fullTree->{module}}) {

    $self->{moduleHash}->{$module->{name}} = {};

    foreach my $sqlQuery (@{$module->{sqlQuery}}) {
      if ($sqlQuery->{includeProjects} && $sqlQuery->{excludeProjects}) {
	die "\n<sqlQuery name=\"$sqlQuery->{name}\"> has both 'includeProjects=' and 'excludeProjects='\n";
      }

      next if ($sqlQuery->{includeProjects}
	       && $sqlQuery->{includeProjects} !~ /$project/);
      next if ($sqlQuery->{excludeProjects}
	       && $sqlQuery->{excludeProjects} =~ /$project/);

      if ($self->{moduleHash}->{$module->{name}}->{$sqlQuery->{name}}) {
	die "\n<sqlQuery name=\"$sqlQuery->{name}\"> is included more than once for $project\n"
      }

      $self->{moduleHash}->{$module->{name}}->{$sqlQuery->{name}} = $sqlQuery;
      push(@{$self->{sqlQueryArray}}, $sqlQuery);
    }
  }
}

# used by gbrowse adaptor
sub getSQL {
  my $self = shift;
  my ($moduleName, $sqlQueryName) = @_;

  return $self->{moduleHash}->{$moduleName}->{$sqlQueryName}->{sql}->[0];
}

# used by sanity test
sub getSqlStringFromSqlQuery {
  my ($self, $sqlQuery) = @_;
  return $sqlQuery->{sql}->[0];
}

# used by sanity test
sub getSqlQueryArray {
  my ($self) = @_;
  return $self->{sqlQueryArray}
}

# used by sanity test
sub getSanityTestInfo {
  my ($self) = @_;
  return $self->{sanityTestInfo};
}

sub print {
  my $self = shift;

  my $tree = $self->getTree();

  print Dumper($tree);
}

# used by orthomcl
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
