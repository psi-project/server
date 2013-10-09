{
	"name" :		"kmeans",
	"description" :	"K Means clustering algorithm",
	"implementation" :	"weka.clusterers.SimpleKMeans",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to determine clusters" } },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"k" :	{
			"description":	"Number of clusters",
			"type":			"$integer",
			"constraints":	{ "min" : 2 },
			"toolkitName":	"-N",
			"defaultValue": 2
		},
		//-V option ignored since algorithm doesn't have ability to 'display' anything to client
		"use_mean_for_missing" :	{
			"description":	"Replace missing values with mean/mode",
			"toolkitName" :	"-M"
		},
		"seed" : {
			"description":	"Random number seed",
			"type":			"$integer",
			"toolkitName":	"-S",
			"defaultValue":	10
		},
		"distance_function" : {
			"description":	"Distance function to be used for instance comparison",
			"type":			"$string",
			"constraints":	{ "enum": [ "weka.core.EuclideanDistance", "weka.core.ChebyshevDistance", "weka.core.ManhattanDistance" ] }, 
			"defaultValue":	"weka.core.EuclideanDistance",	
			"toolkitName":	"-A"
		},
		"iterations" : {
			"description":	"Maximum number of iterations",
			"type":			"$integer",
			"constraints":	{ "min" : 1 },
			"toolkitName":	"-I",
			"defaultValue":	500
		}
		//-O option left out; what effect does preserving the order of instances mean in a PSI system?
	}
}
