(: XQuery file to return a full ead record :)
module namespace page = 'http://basex.org/examples/web-page';
declare default element namespace "urn:isbn:1-931666-22-9";

declare
  %rest:path("/create/{$database}")
  %rest:single
  %rest:GET
updating function page:createDatbase($database) {
	 if (db:exists($database)) then (

	 )	 else (
		 db:create($database)
	 )
	 
};
