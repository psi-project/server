/* Internal definition of the Iris relation, together with its attribute(s). */
{
	"name":			"jsonIris", //Will become part of its URI
	"description":	"The Iris dataset, courtesy of R.A. Fisher, with instances encoded as objects",
	"format":		"JSON"; //Used internally to create appropriate readers
	"path":			"/private/datasets/jsonIris.data",
	//Definitions of new attributes. Keys are used as their URIs.
	// First attribute will be set as default for this relation;
	// all will have this relation set as their default.
	// Attributes are created in order, so later ones may use
	// earlier ones as sub-attributes.
	"attributes":	[
		{
			"name":			"features",
			"psiType":	"attribute-definition",
			"description":	"An object representation of iris dimensions and species",
			"attribute": {
				"sepal" : {
					"length": "primitive://property/sepal/length?type=number",
					"width":  "primitive://property/sepal/width?type=number"
				},
				"petal": {
					"length": "primitive://property/petal/length?type=number",
					"width": "primitive://property/petal/width?type=number"
				},
				"species": "primitive://property/species?type=string&values=setosa,versicolor,virginica"
			}
		}
	]
}