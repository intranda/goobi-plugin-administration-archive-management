(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:path("/dbname/{$identifier}")
  %rest:single
  %rest:GET
  
function page:getDatabase($identifier) {
  <db>
  {
  let $databases := db:list()
  for $c in $databases
    let $files := db:list-details($c)
    for $filename in $files
      return
      let $ead := db:open($c, $filename)/ead
        return
        if (exists($ead[//c[@level="file"][@id=$identifier]])) then (
          <record database="{$c}" filename="{$filename}" />
        ) else ()
    }
  </db>
};