/* Internal definition of the Iris relation, together with its attribute(s). */
{
	"name":			"iris", //Will become part of its URI
	"description":	"The Iris dataset, courtesy of R.A. Fisher",
	"format":		"CSV"; //Used internally to create appropriate readers
	"path":			"/private/datasets/iris.csv",
	//Definitions of new attributes. Attributes are created in order,
	// so later ones may use earlier ones as sub-attributes.
	"defaultAttribute": "features",
	"attributes":	[
		{
			"name": 		"sepal_length",
			"psiType":	"attribute-definition",
			"description":	"Sepal length of iris flower (in cm)",
			"attribute":	"primitive://csv/1?type=number"
		},
		{
			"name": 		"sepal_width",
			"psiType":	"attribute-definition",
			"description":	"Sepal width of iris flower (in cm)",
			"attribute":	"primitive://csv/2?type=number"
		},
		{
			"name":			"petal_length",
			"psiType":	"attribute-definition",
			"description":	"Petal length of iris flower (in cm)",
			"attribute":	"primitive://csv/3?type=number"
		},
		{
			"name":			"petal_width",
			"psiType":	"attribute-definition",
			"description":	"Petal width of iris flower (in cm)",
			"attribute":	"primitive://csv/4?type=number"
		},
		{
			"name":			"species",
			"psiType":	"attribute-definition",
			"description":	"Iris flower species",
			"attribute":	"primitive://csv/5?type=string&values=setosa,versicolor,virginica"
		},
		{
			"name":			"features",
			"psiType":	"attribute-definition",
			"description":	"A feature vector representation of iris dimensions and species",
			"attribute": [
				"local://localhost/data/iris/sepal_length",
				"local://localhost/data/iris/sepal_width",
				"local://localhost/data/iris/petal_length",
				"local://localhost/data/iris/petal_width",
				"local://localhost/data/iris/species"
			]
		},
		{
			"name":			"featuresNoClass",
			"psiType":	"attribute-definition",
			"description":	"A feature vector representation of iris dimensions",
			"attribute": [
				"local://localhost/data/iris/sepal_length",
				"local://localhost/data/iris/sepal_width",
				"local://localhost/data/iris/petal_length",
				"local://localhost/data/iris/petal_width"
			]
		}//,
//		"asObjectDirect": {
//			"psiType":	"attribute-definition",
//			"description":	"An object representation of iris dimensions",
//			"attribute": {
//				"sepal": {
//					"length":	"primitive://csv/1?type=number",
//					"width":	"primitive://csv/2?type=number"
//				},
//				"petal": {
//					"length":	"primitive://csv/3?type=number",
//					"width":	"primitive://csv/4?type=number"
//				},
//				"species":	"primitive://csv/5?type=string&values=setosa,versicolor,virginica"
//			}
//		},
//		"asObjectFromFeatures": {
//			"psiType":	"attribute-definition",
//			"description":	"An object representation of iris dimensions that uses the user-defined array-valued iris attribute",
//			"attribute": {
//				"sepal": {
//					"length":	"local://localhost/data/iris/features/1",
//					"width":	"local://localhost/data/iris/features/2"
//				},
//				"petal": {
//					"length":	"local://localhost/data/iris/features/3",
//					"width":	"local://localhost/data/iris/features/4"
//				},
//				"species":	"local://localhost/data/iris/features/5"
//			}
//		},
//		"asObjectFromObject": {
//			"psiType":	"attribute-definition",
//			"description":	"An object representation of iris dimensions that uses the structured attributes of asObjectFromFeatures to define its own sub-attributes",
//			"attribute": {
//				"s":	"local://localhost/data/iris/asObjectFromFeatures/sepal",
//				"p":	"local://localhost/data/iris/asObjectFromFeatures/petal",
//				"sp":	"local://localhost/data/iris/asObjectFromFeatures/species"
//			}
//		}
	]
}
