{
	"name" :		"neural_network",
	"description" :	"Multilayer Perceptron (i.e., an artificial neural network) that uses backpropogation algorithm to train the network",
	"implementation" :	"weka.classifiers.functions.MultilayerPerceptron",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "oneOf": [
			{ "$numberAttribute" : { "description": "The value label of each instance (i.e., what will be predicted)" } },
			{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } }
		] },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"learning_rate" : {
			"description":	"Learning rate for the backpropagation algorithm",
			"type":			"$number",
			"constraints":	{ "min" : 0, "max": 1 },
			"defaultValue":	0.3,
			"toolkitName":	"-L"
		},
		"momentum" : {
			"description":	"Momentum Rate for the backpropagation algorithm",
			"type":			"$number",
			"constraints":	{ "min" : 0, "max": 1 },
			"defaultValue":	0.2,
			"toolkitName":	"-M"
		},
		"epochs" : {
			"description":	"Number of epochs to train through",
			"type":			"$integer",
			"constraints":	{ "min" : 1},
			"defaultValue":	500,
			"toolkitName":	"-N"
		},
		"seed" : {
			"description":	"Seed for random number generator",
			"type":			"$integer",
			"constraints":	{ "min" : 0 },
			"defaultValue":	0,
			"toolkitName":	"-S"
		},
		"allowed_errors" : {
			"description":	"Number of consecutive errors allowed for validation testing before training terminates",
			"type":			"$integer",
			"constraints":	{ "min" : 1},
			"defaultValue":	20,
			"toolkitName":	"-E"
		},
		"do_not_normalize_inputs" : {
			"description":	"Do not normalise inputs",
			"toolkitName":	"-I"
		},
		"hidden_layers" : {
			"description":	"Specification of the number of nodes in the hidden layers in the network. Must be a comma-separated list of positive integers or make use of the following characters: 'a' = (attributes + classes) / 2; 'i' = attributes; 'o' = classes; 't' = attributes + classes.",
			"type":			"$string",
			"constraints":	{ "min" : 1, "pattern": "^([1-9]\\d*|[aiot])(,([1-9]\\d*|[aiot]))*$" },
			"defaultValue":	"a",
			"toolkitName":	"-H"
		},
		"decay_learning_rate" : {
			"description":	"Reduce the learning rate over time",
			"toolkitName":	"-D"
		}
	}
// -C
//  Normalizing a numeric class will NOT be done.
//  (Set this to not normalize the class if it's numeric).
}   
