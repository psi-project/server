/* Internal definition of a pairwise preferences relation based on Iris flowers, together with its attribute(s). */
{
	"name":			"iris_prefs_full",
	"description":	"All pairs of iris flowers from the iris dataset, where one is preferred to the other. Setosa is preferred to the other species, versicolor is preferred to virginica.",
	"format":		"CSV";
	"path":			"/private/datasets/iris_prefs_full.csv",
	"defaultAttribute": "pair",
	"attributes":	[
		{
			"name": 		"preferred",
			"psiType":	"attribute-definition",
			"description":	"Sepal and petal measurements for the preferred flower (in metrics) and its species",
			"attribute":	{
				"metrics":	[
					"primitive://csv/1?type=number",
					"primitive://csv/2?type=number",
					"primitive://csv/3?type=number",
					"primitive://csv/4?type=number"
				],
				"species": "primitive://csv/5?type=string&values=setosa,versicolor,virginica"
			}
		},
		{
			"name": 		"not_preferred",
			"psiType":	"attribute-definition",
			"description":	"Sepal and petal measurements for the less preferred flower (in metrics) and its species",
			"attribute":	{
				"metrics":	[
					"primitive://csv/6?type=number",
					"primitive://csv/7?type=number",
					"primitive://csv/8?type=number",
					"primitive://csv/9?type=number"
				],
				"species": "primitive://csv/10?type=string&values=setosa,versicolor,virginica"
			}
		},
		{
			"name": 		"pair",
			"psiType":	"attribute-definition",
			"description":	"Pair of iris flowers, where one is preferred to the other",
			"attribute":	{
				"preferred": "local://localhost/data/iris_prefs_full/preferred",
				"not_preferred": "local://localhost/data/iris_prefs_full/not_preferred"
			}
		}
	]
}
