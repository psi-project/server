{
	"name" :		"gmm",
	"description" :	"Gaussian Mixture Model",
	"implementation" :	"mixture.GMM",
	"learnerModelClass" : "models.learner.SKLearnLearner",
	"toolkitOptions":	{
		"isUpdatable": false
	},
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$numberSchema", "description": "Instance details that will be used to determine clusters" } }
	},
	"parameters" :	{
		"n_components" :	{
			"description":	"Number of mixture components.",
			"type":			"$integer",
			"constraints":	{ "min" : 1 },
			"defaultValue": 1,
			"toolkitName":	"n_components"
		},
		"covariance_type" : {
			"description":	"The type of covariance parameters to use: 'spherical', 'tied', 'diag' or 'full'.",
			"type":			"$string",
			"constraints":	{ "enum": [ "spherical", "tied", "diag", "full" ] },
			"defaultValue": "diag",
			"toolkitName":	"covariance_type"
		},
		"min_covar" :	{
			"description":	"Floor on the diagonal of the covariance matrix to prevent overfitting.",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 1e-3,
			"toolkitName":	"min_covar"
		},
		"threshold" :	{
			"description":	"The convergence threshold.",
			"type":			"$number",
			"constraints":	{ "min" : 0, "exclusiveMinimum" : true },
			"default":		0.01,
			"toolkitName":	"thresh"
		},
		"n_iter" :	{
			"description":	"The number of EM iterations to perform.",
			"type":			"$integer",
			"constraints":	{ "min" : 1 },
			"toolkitName":	"n_iter"
		},
		"n_init" :	{
			"description":	"The number of initializations to perform; the best result is kept.",
			"type":			"$integer",
			"constraints":	{ "min" : 1 },
			"toolkitName":	"n_init"
		},
		"params" : {
			"description":	"Which parameters are updated in the training process. Can contain any combination of 'w' for weights, 'm' for means, and 'c' for covars (the default is all three).",
			"type":			"$string",
			"constraints":	{ "pattern": "[wmc]*" },
			"defaultValue": "wmc",
			"toolkitName":	"params"
		},
		"init_params" : {
			"description":	"Which parameters are updated in the initialization process. Can contain any combination of 'w' for weights, 'm' for means, and 'c' for covars (the default is all three).",
			"type":			"$string",
			"constraints":	{ "pattern": "[wmc]*" },
			"defaultValue": "wmc",
			"toolkitName":	"init_params"
		}
	}
}
