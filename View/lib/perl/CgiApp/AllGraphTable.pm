package ApiCommonWebsite::View::CgiApp::AllGraphTable;
@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

sub run {
  my ($self, $cgi) = @_;

  my $prefix = $cgi->param('prefix');

  print STDOUT $cgi->header(); 
 
  my $dbh = $self->getQueryHandle($cgi);

  my $sql = "
  select distinct ds.project_id
                         ,dsp.dataset_presenter_id
                         ,dsp.NAME
                         ,dmr.TARGET_NAME
                         ,desi.example_source_id as source_id
                         ,prop.value as graphModule 
  from apidbtuning.datasetmodelref dmr
           ,apidbtuning.datasetPresenter dsp
           ,apidbtuning.datasetProperty prop
           ,apidbtuning.datasetExampleSourceId desi
           ,apidb.dataSource ds
  where dmr.dataset_presenter_id = dsp.dataset_presenter_id
      and prop.dataset_presenter_id = dsp.dataset_presenter_id
      and ( dsp.name = ds.name or ds.name like dsp.dataset_name_pattern)
      and ds.name=desi.dataset 
      and prop.property = 'graphModule'
      and dmr.target_type =  'profile_graph'
";

  my $sth = $dbh->prepare($sql);
  $sth->execute();

  
  my $data = {};
  my $base_url = "$prefix.";
  
  print  STDOUT $cgi->hr;
  print  STDOUT $cgi->start_table({border=>'1', cellspacing=>'0', cellpadding=>'1'});
  while (my ($projectId, $datasetId,$datasetName, $graphName, $sourceId,$graphModule) = $sth->fetchrow_array) {
    my $graph_url = "http://$prefix.$projectId.org/cgi-bin/dataPlotter.pl?type=$graphModule&project_id=$projectId&datasetId=$datasetId&template=1&id=$sourceId"; 
    print STDOUT    $cgi->Tr(
                             $cgi->td($projectId),
                             $cgi->td($datasetName),
                             $cgi->td($cgi->img({
                                                 -src => $graph_url,  
                                                 -alt => $graph_url,
                                                }),
                                     )
                             );
  }

  print STDOUT $cgi->end_table;
  $sth->finish();
  print STDOUT $cgi->end_html();

}

1;
