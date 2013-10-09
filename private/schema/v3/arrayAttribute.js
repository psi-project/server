{ 
    "/psiType=":   "attribute",
    "/uri":             "$uri",
    "?description":     "$string",
    "/emits": {
        "/type=": "array",
        "/items": { "$array": { "allItems": "%allItems" } }
    },
    "?relation":		"$uri",
    "?subattributes": 	{ "$array": { "allItems": "$uri" } },
    "?querySchema":		"$object"
}