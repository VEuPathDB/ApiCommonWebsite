// __EUPATHDB_CONFIG__ is defined in index.jsp
export let {
  rootElement,    // comes from pageFrame
  rootUrl,        // comes from deployment descriptor
  endpoint,       // comes from deployment descriptor
  projectId,      // comes from deployment descriptor
  webAppUrl,      // comes from deployment descriptor
  buildNumber,    // comes from model xml
  releaseDate,    // comes from model xml
  facebookId,     // comes from config
  twitterId,      // comes from config
  youtubeId,      // comes from config
  announcements
} = window.__EUPATHDB_CONFIG__;

// Question name and search param to use for quick search boxes in header
// TODO Put these in config
export let quickSearches = [
  { name: 'GeneBySingleLocusTag', quickSearchParamName: 'single_gene_id', quickSearchDisplayName: 'Gene ID' },
  { name: 'GenesByTextSearch', quickSearchParamName: 'text_expression', quickSearchDisplayName: 'Gene Text Search'}
];
