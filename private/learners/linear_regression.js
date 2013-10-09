{
	"name" :		"linear_regression",
	"description" :	"Linear Regression",
	"implementation" :	"weka.classifiers.functions.LinearRegression",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$numberAttribute" : { "description": "The value (i.e., y-value, dependent variable value) of each instance (i.e., what will be predicted)" } },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"selection_method" : {
			"description":	"Attribute selection method (0 = use M5 method, 1 = no selection, 2 = greedy selection)",
			"type":			"$integer",
			"toolkitName":	"-S",
			"constraints":	{ "enum": [ 0, 1, 2 ] },
			"defaultValue": 0
		},
		"leave_colinear" :	{
			"description":	"Do not try to eliminate colinear attributes",
			"toolkitName":	"-C"
    	},
    	"ridge" :	{
    		"description":	"Value of the ridge parameter (the relative emphasis of ridge regression versus standard linear regression)",
    		"type":			"$number",
    		"constraints":	{ "min" : 0 },
    		"defaultValue":	1e-8,
    		"toolkitName":	"-R"
    	}
	}
}
    
