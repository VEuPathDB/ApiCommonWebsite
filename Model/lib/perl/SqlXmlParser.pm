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
  my $fullTree = $xsl->XMLin($file, Cache => 'memshare', forcearray => 1, keyattr => { module => '+name'}) or die "\ncannot open the sql file\n";

  print Dumper $fullTree if ($showParse);

  $self->{prunedTree} = &pruneTree($fullTree, $project);

  bless( $self, $class );
  return $self;
}

sub getTree {$_[0]->{prunedTree}}

sub print {
  my $self = shift;

  my $tree = $self->getTree();

  print Dumper($tree);
}

# remove exluded sqlQueries, and change shape from array of sqlQueries to hash
sub pruneTree {
  my ($fullTree, $project) = @_;

  my $prunedTree = {};

  $prunedTree->{sanityTestInfo} = $fullTree->{sanityTestInfo};

  foreach my $moduleName (keys(%{$fullTree->{module}})) {
    my $module = $fullTree->{module}->{$moduleName};

    $prunedTree->{module}->{$moduleName} = {};

    foreach my $sqlQuery (@{$module->{sqlQuery}}) {
      if ($sqlQuery->{includeProjects} && $sqlQuery->{excludeProjects}) {
	die "\n<sqlQuery name=\"$sqlQuery->{name}\"> has both 'includeProjects=' and 'excludeProjects='\n";
      }

      next if ($sqlQuery->{includeProjects}
	       && $sqlQuery->{includeProjects} !~ /$project/);
      next if ($sqlQuery->{excludeProjects}
	       && $sqlQuery->{excludeProjects} != /$project/);

      die "\n<sqlQuery name=\"$sqlQuery->{name}\"> is included more than once for $project\n"
	if $prunedTree->{module}->{$moduleName}->{sqlQuery}->{$sqlQuery->{name}};

      $prunedTree->{module}->{$moduleName}->{sqlQuery}->{$sqlQuery->{name}} = $sqlQuery;
    }
  }
  return $prunedTree;
}

sub getSQL {
  my $self = shift;
  my ($moduleName, $sqlQueryName) = @_;

  return $self->{prunedTree}->{$moduleName}->{$sqlQueryName}->{sql}->[0];
}


sub getSqlStringFromSqlQuery {
  my ($self, $sqlQuery) = @_;
  return $sqlQuery->{sql}->[0];
}

1;
