module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare 
%rest:GET 
%rest:path("/updateNode/{$database}/{$xmlDocument}/{$nodeid}")
    
updating function page:updateNode($database, $xmlDocument, $nodeid) { 

  (: read new node content from file system:)
  let $path := file:resolve-path($nodeid, '/opt/digiverso/basex/import/')
  let $newContent :=  doc($path)

  (: open selected file :)
  let $ead := db:get($database, $xmlDocument)/ead

  (: find node to update :)
  let $oldNode := $ead//c[@id=$nodeid]

  (: replace node:)
  for $node in $oldNode
    return replace node $node with $newContent  
};