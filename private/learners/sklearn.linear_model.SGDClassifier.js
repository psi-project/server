{
	"name" :		"sgd_classifier",
	"description" :	"Stochastic Gradient Descent Classifier",
	"implementation" :	"linear_model.SGDClassifier",
	"learnerModelClass" : "models.learner.SKLearnLearner",
	"toolkitOptions":	{
		"isUpdatable": true
	},
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$numberSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } }
	},
	"parameters" :	{
		"loss" : {
			"description":	"The loss function to use: 'hinge', 'perceptron', 'log', 'modified_huber', 'squared_loss', 'huber' or 'epsilon_insensitive'.",
			"type":			"$string",
			"constraints":	{ "enum": [ "hinge", "perceptron", "log", "modified_huber", "squared_loss", "huber", "epsilon_insensitive" ] },
			"defaultValue": "hinge",
			"toolkitName":	"loss"
		},
		"penalty" : {
			"description":	"The penalty or regularization term: 'l1', 'l2' or 'elasticnet'.",
			"type":			"$string",
			"constraints":	{ "enum": [ "l1", "l2", "elasticnet" ] },
			"defaultValue": "l2",
			"toolkitName":	"penalty"
		},
		"alpha" :	{
			"description":	"Constant multiplier for regularization term",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 0.0001,
			"toolkitName":	"alpha"
    	},
		"l1_ratio" :	{
			"description":	"The Elastic Net mixing parameter, 0 < l1_ratio <= 1.",
			"type":			"$number",
			"constraints":	{ "min" : 0, "max" : 1 },
			"defaultValue": 0.15,
			"toolkitName":	"rho"
    	},
		"fit_intercept" :	{
			"description":	"Whether the intercept should be estimated or not. If false, the data is assumed to be already centered.",
			"defaultValue": true,
			"toolkitName":	"fit_intercept"
    	},
		"n_iter" :	{
			"description":	"The number of passes over the training data (i.e., 'epochs')",
			"type":			"$integer",
			"constraints":	{ "min" : 1 },
			"defaultValue": 5,
			"toolkitName":	"n_iter"
    	},
		"shuffle" :	{
			"description":	"Whether or not the training data should be shuffled after each epoch.",
			"defaultValue": false,
			"toolkitName":	"shuffle"
    	},
		"seed" :	{
			"description":	"Seed for the random number generator used when shuffling the data.",
			"type":			"$integer",
			"toolkitName":	"random_state"
    	},
//		"verbose" has no effect as processing hidden
    	"epsilon":	{
    		"description" : "Epsilon in the epsilon-insensitive loss functions; only if loss=='huber' or loss='epsilon_insensitive'. If the difference between the current prediction and the correct label is below this threshold then the model is not updated.",
    		"type":			"$number",
    		"constraints":	{ "min" : 0 },
    		"defaultValue":	0.1,
    		"toolkitName":	"epsilon"
    	},
//		"n_jobs" shoudn't be user-controllable as that's a server-side decision
		"learning_rate" : {
			"description":	"The learning rate: 'constant' means eta = eta0; 'optimal' means eta = 1.0/(t+t0); and 'invscaling' means eta = eta0 / pow(t, power_t).",
			"type":			"$string",
			"constraints":	{ "enum": [ "constant", "optimal", "invscaling" ] },
			"defaultValue": "optimal",
			"toolkitName":	"learning_rate"
		},
		"eta0" :	{
			"description":	"The initial learning rate",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 0.01,
			"toolkitName":	"eta0"
    	}
//		"class_weight" cannot yet be supported
//		"warm_start" is not appropriate for a stateless system
	}
}
