package ApiCommonWebsite::View::CgiApp::GeneImage;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/html');

  $self->handleImage($dbh, $cgi);

  exit();
}

sub handleImage {
  my ($self, $dbh, $cgi) = @_;

  my $source_id  = $cgi->param('source_id');

  my $sql = "";

  $sql = <<EOSQL;
SELECT substr(uri, 1, instr(uri,',', 1,1)-1) as dic_img_uri,
     substr(uri, instr(uri,',', 1,1)+1) as gfp_img_uri,
     note, source_id
     FROM (
     SELECT listagg(image_uri, ',') within group (order by image_type) as uri,
     replace(note, 'Gene annotation: ', '') as note, source_id
     FROM
     ( SELECT distinct img.image_uri, replace(img.note, 'GO term: ', '') as note,
     img.label_type, img.image_type, gf.source_id
     FROM ApiDB.NAFeatureImage img, Dots.GeneFeature gf
     WHERE img.na_feature_id = gf.na_feature_id
     ) a
     group by source_id, note
     )
     where lower(source_id) = lower('$source_id')
EOSQL


  my $sth = $dbh->prepare($sql);
  $sth->execute();


  print "<table align=center width=800>";
  while(my ($dic_img_uri, $gfp_img_uri, $note, $source_id) = $sth->fetchrow_array()) {
     print <<EOL
      <tr>
       <td colspan=2>
         $source_id - $note
       </td>
     </tr>
     <tr>

       <td>
       <img src="/common/GintestinalisAssemblageA/image/gassAWB_DBP_GeneImage_RSRC/$gfp_img_uri.jpg">

      </td>

       <td>
       <img src="/common/GintestinalisAssemblageA/image/gassAWB_DBP_GeneImage_RSRC/$dic_img_uri.jpg">

      </td>

     <tr>

       <td>
       <a href="/common/GintestinalisAssemblageA/image/gassAWB_DBP_GeneImage_RSRC/$gfp_img_uri">Download GFP image in TIFF format</a>
      </td>

       <td>
       <a href="/common/GintestinalisAssemblageA/image/gassAWB_DBP_GeneImage_RSRC/$dic_img_uri">Download DIC image in TIFF format</a>

      </td>

     </tr>
EOL
  }
  print "</td></tr>";
  print "</table>";

}

1;
