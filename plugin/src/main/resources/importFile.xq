(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:GET
  %rest:path("/import/{$db}/{$filename}")

updating function page:import($db, $filename) {
  let $path := '/opt/digiverso/basex/import/' || $filename
  let $details := db:list-details($db, $filename)

  return 
    if (fn:empty($details)) then
      db:add($db, doc($path), $filename)
    else 
      db:replace($db, $filename, doc($path))
};