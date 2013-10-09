{
	"name" :		"svc",
	"description" :	"C-Support Vector Classification",
	"implementation" :	"svm.SVC",
	"learnerModelClass" : "models.learner.SKLearnLearner",
	"toolkitOptions":	{
		"isUpdatable": false
	},
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$numberSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } }
	},
	"parameters" :	{
		"C" :	{
			"description":	"Penalty parameter C of the error term. If not specified then it is set to the number of samples",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"toolkitName":	"C"
    	},
		"kernel" : {
			"description":	"The kernel type to be used in the algorithm: 'linear', 'poly', 'rbf', 'sigmoid', or 'precomputed'.",
			"type":			"$string",
			"constraints":	{ "enum": [ "linear", "poly", "rbf", "sigmoid", "precomputed" ] },
			"defaultValue": "rbf",
			"toolkitName":	"kernel"
		},
		"degree" :	{
			"description":	"The degree of the kernal function (if 'poly' or 'sigmoid')",
			"type":			"$integer",
			"constraints":	{ "min" : 0 },
			"defaultValue": 3,
			"toolkitName":	"degree"
    	},
		"gamma" :	{
			"description":	"Kernel coefficient for 'rbf' and 'poly' kernels. If set to 0.0 then 1/(number of features) will be used.",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 0.0,
			"toolkitName":	"gamma"
    	},
		"coef0" :	{
			"description":	"Independent term in kernel function; used if kernel is 'poly' or 'sigmoid'",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 0.0,
			"toolkitName":	"coef0"
    	},
//    	"probability" is inaccessible through PSI interface later, so don't let user know about it now
		"shrinking" :	{
			"description":	"Use the shrinking heuristic",
			"defaultValue": true,
			"toolkitName":	"shrinking"
    	},
    	"tol" :	{
			"description":	"Tolerance for stopping criterion",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 1e-3,
			"toolkitName":	"tol"
    	}
//    	"cache_size" appears to low level (and open to user abuse) to expose
//    	"class_weight" probably *should* be supported, but is not very easy, since *either* string or a dictionary
//    	"verbose" is irrelevant as processing hidden from client
	}
}
    
