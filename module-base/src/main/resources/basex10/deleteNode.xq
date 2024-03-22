module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare 
%rest:GET 
%rest:path("/deleteNode/{$database}/{$xmlDocument}/{$nodeid}")
    
updating function page:deleteNode($database, $xmlDocument, $nodeid) { 

  (: open selected file :)
  let $ead := db:get($database, $xmlDocument)/ead

  (: find node :)
  let $node := $ead//c[@id=$nodeid]

  (: delete node:)
    return delete node $node
};