{ 
	"/psiType=": 		"attribute",
    "/uri":    	        "$uri",
    "?description": 	"$string",
    "/emits":			"$object",
    "?relation":		"$uri",
	"?subattributes": 	{
		"oneOf" : [
			{ "$array": { "allItems": "$uri" } },
			{ "/*" : "$uri" }
		]
	},
	"?querySchema":      "$object"
}
