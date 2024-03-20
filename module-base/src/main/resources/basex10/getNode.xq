module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare 
%rest:GET 
%rest:path("/getNode/{$database}/{$xmlDocument}/{$nodeid}")
    
function page:getNode($database, $xmlDocument, $nodeid) { 

  (: open selected file :)
  let $ead := db:get($database, $xmlDocument)/ead

  (: find node :)
  let $node := $ead//c[@id=$nodeid]

  (: return node:)
    return $node
};