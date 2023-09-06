(: XQuery file to return an ead node, all ancestors and direct children :)
module namespace page = 'http://basex.org/examples/web-pagepage';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:path("/ancestor/{$identifier}")
  %rest:single
  %rest:GET
function page:getAncestor($identifier) {
let $ead := db:get('hu')/ead[//c[@id=$identifier]]
let $archdesc := $ead/archdesc
let $dsc := $archdesc/dsc
let $record :=$ead//c[@id=$identifier]

return 
    <ead>
    
{$ead/eadheader}
  <archdesc>
{$archdesc/did}
 {page:createC($record/ancestor::c, $record)}
</archdesc>
    
  </ead>

};


declare function page:createC($stack, $record) {
if(empty($stack)) then
( 
(: add current element :)

<c level="{data($record/@level)}" id="{data($record/@id)}">
   {$record/did}
   {$record/accessrestrict}
   {$record/otherfindaid}
   {$record/odd}
   {$record/scopecontent}
   {$record/index}


{for $element in $record/c
return
page:createChild($element)
}

   </c>

)  else (
let $c := $stack[1]
return
 (: create parent structure :)
     <c level="{data($c/@level)}" id="{data($c/@id)}">
   {$c/did}
   {$c/accessrestrict}
   {$c/otherfindaid}
   {$c/odd}
   {$c/scopecontent}
   {$c/index}

   {page:createC(remove($stack,1), $record)}

</c>
)
};

declare function page:createChild($record) {
 (
<c level="{data($record/@level)}" id="{data($record/@id)}">
   {$record/did}
   {$record/accessrestrict}
   {$record/otherfindaid}
   {$record/odd}
   {$record/scopecontent}
   {$record/index}
</c>
)
};


  
