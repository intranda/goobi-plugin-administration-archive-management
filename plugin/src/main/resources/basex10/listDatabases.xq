(: XQuery file to return the names of all available databases :)
module namespace page = 'http://basex.org/examples/web-pagepage';
(:declare default element namespace "urn:isbn:1-931666-22-9";:)

declare
  %rest:path("/databases")
  %rest:single
  %rest:GET
function page:getDatabases() {
let $ead := db:list()
return 
    <databases>
{
for $c in $ead
 return 
<database>
<name>
{$c}
</name>

{
let $files := db:list-details($c)

return
<details>
{$files}
</details>
}

</database>

}

  </databases>

};



  

