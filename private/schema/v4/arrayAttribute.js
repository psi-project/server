{ 
    "allOf" : [ "$attribute" ],
    "/emits": {
        "/type=": "array",
        "/items": { "$array": { "allItems": "%allItems" } }
    }
}