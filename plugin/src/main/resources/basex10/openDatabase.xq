(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:path("/db/{$database}/{$filename}")
  %rest:single
  %rest:GET
function page:getDatbase($database, $filename) {
let $ead := db:get($database, $filename)/ead
return 
<collection>
  {$ead}
</collection>
};
