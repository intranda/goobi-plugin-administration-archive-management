module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare 
%rest:GET 
%rest:path("/swapNodes/{$database}/{$xmlDocument}/{$firstNodeId}/{$secondNodeId}")
    
function page:swapNodes($database, $xmlDocument, $firstNodeId, $secondNodeId) { 

  (: open selected file :)
  let $ead := db:get($database, $xmlDocument)/ead

  (: find node to move :)
  let $firstNode := $ead//*[@id=$firstNodeId]
  let $secondNode := $ead//*[@id=$secondNodeId]
  

 let $var :=   page:deleteNode($firstNode)
 let $var2 := page:insertNode($firstNode, $secondNode)

return 
<collection>
  {$firstNode/..}
</collection>
};

declare function page:deleteNode($node) {
    delete node $node
};

declare function page:insertNode($firstNode, $secondNode) {
    insert node $firstNode after $secondNode
};