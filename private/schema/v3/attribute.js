{ 
	"/psiType=": 		"attribute",
    "/uri":    	        "$uri",
    "?description": 	"$string",
    "/emits":			"$object",
    "?relation":		"$uri",
    "?subattributes": 	{
    	"type": [ "array", "object" ],
    	"items": "$uri",
    	"additionalProperties": "$uri"
    },
    "?querySchema":		"$object"
}