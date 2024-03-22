module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare 
%rest:GET 
%rest:path("/moveNode/{$database}/{$xmlDocument}/{$nodeid}/{$parentid}")
    
function page:moveNode($database, $xmlDocument, $nodeid, $parentid) { 

  (: open selected file :)
  let $ead := db:get($database, $xmlDocument)/ead

  (: find node to move :)
  let $node := $ead//*[@id=$nodeid]

  (: find new parent node :)
  
  let $destination := $ead//*[@id=$parentid]

 let $var := page:deleteNode($node)
  let $var2 :=  page:insertNode($node, $destination)

return $destination
};

declare updating function page:deleteNode($node) {
  update:output("delete"), delete node $node
};

declare updating function page:insertNode($node, $destination) {
  update:output("insert"), insert node $node into $destination 
};