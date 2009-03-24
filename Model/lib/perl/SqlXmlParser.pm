package ApiCommonWebsite::Model::SqlXmlParser;

use strict;
use XML::Simple;
use Data::Dumper;

sub new {
	my $class = shift;
	my $self = {};
	my @file = @_;

	my $xsl = XML::Simple->new();
	my $tree = $xsl->XMLin(@file, Cache => 'memshare') or die "cannot open the sql file\n";

	$self->{tree} = $tree;

	bless( $self, $class );
	return $self;

}

sub getTree {$_[0]->{tree}}

sub setProjectId {$_[0]->{projectId} = $_[1]}
sub getProjectId {
  my ($self) = @_;

  my $projectId = $self->{projectId};

  unless($projectId) {
    die "Project Id was not set!";
  }
  return $projectId;
}

sub getSQL {
	my $self = shift;
	my ($modulename, $key) = @_;

	my $obj = $self->{tree}->{module}->{$modulename}->{sqlQuery};

        return $obj->{sql} if (exists $obj->{sql});
        my $sqlObj = $obj->{$key}->{sql};

        return $self->_getSQL($sqlObj, $key);
}


sub print {
	my $self = shift;

        my $tree = $self->getTree();

	print Dumper($tree->{module}->{'GUS.pm'});
}

sub _getSQL {
  my ($self, $sqlObj, $key) = @_;

  my $projectId = $self->getProjectId();

  my $rv;
  my $sqlCount = 0;

  # If there is only one but it has an includeProjects attribute force it onto an array
  if(ref($sqlObj) eq 'HASH') {
    $sqlObj = [$sqlObj];
  }

  if(ref($sqlObj) eq 'ARRAY') {
    my $alreadyIncluded;

    foreach my $sql (@$sqlObj) {
      if(ref($sql) eq 'HASH') {
        next if($sql->{excludeProjects} && $sql->{excludeProjects} =~ /$projectId/);

        if($sql->{includeProjects} && $sql->{includeProjects} =~ /$projectId/) {
          $rv = $sql->{content};
          $alreadyIncluded = 1;
          $sqlCount++;
        }
        if($sql->{excludeProjects} && $sql->{excludeProjects} !~ /$projectId/) {
          $rv = $sql->{content} unless($alreadyIncluded);
          $sqlCount++;
        }
      }
      else {
        $rv = $sql unless($alreadyIncluded);
        $sqlCount++;
      }
    }
  }
  else {
    $rv = $sqlObj;
    $sqlCount++;
  }

  if($sqlCount > 1) {
    die "Multiple SQL statements found for $key" . Dumper $rv;
  }

  return $rv;

}


sub getQueryNamesHash {
  my ($self) = @_;

  my $tree = $self->getTree();
  my @modules = keys %{$tree->{module}};

  my %queryNames;
  foreach my $module (@modules) {
    my $sqlQueryHash = $tree->{module}->{$module}->{sqlQuery};

    # Handle the GUS.pm Case where there is only one query
    if($sqlQueryHash->{sql} && $sqlQueryHash->{name}) {
      my $queryName = $sqlQueryHash->{name};
      push @{$queryNames{$module}}, $queryName;
    }

    else {
      foreach my $queryName (keys %{$tree->{module}->{$module}->{sqlQuery}}) {
        push @{$queryNames{$module}}, $queryName;
      }
    }
  }

  return \%queryNames;
}



#sub parseSqlXmlFile {
#  my($sqlXmlFile, $showParse) = @_;

#  open(FILE, $sqlXmlFile) || die "can't open sql xml file '$sqlXmlFile' for reading\n";
#  my $simple = XML::Simple->new();

#  # use forcearray so elements with one child are still arrays
#  # and, use keyattr so that handlers are given as an ordered list
#  # rather than a hash with name as key.  the ordering is needed
#  # so that undo operations are ordered.  also, the qualifiers retain
#  # the ordering found in the xml file.
#  my $data = $simple->XMLin($sqlXmlFile,
#			    forcearray => 1,
#	      	    KeyAttr => {});
#  if ($showParse) {
#    print Dumper($data);
#    print  "\n\n\n";
#  }
#  return $data;
#}

1;
