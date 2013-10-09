//Interface to our simple ranking algorithm based on logistic regression in scikit-learn
{
	"name" :		"prank",
	"description" :	"Simple preference function learning algorithm. Uses logistic regression on pairs of preferred-less preferred instances to induce the preference function for single instances",
	"implementation" :	"ranking",
	"learnerModelClass" : "models.learner.SKLearnLearner",
	"toolkitOptions":	{
		"isUpdatable": false
	},
	"resources" :	{
		"/preferred" :		{ "$arrayAttribute" : { "allItems": "$numberSchema", "description": "Instance details of the preferred instance in a pair" } },
		"/not_preferred" :	{ "$arrayAttribute" : { "allItems": "$numberSchema", "description": "Instance details of the less preferred instance in a pair" } }
	},
	"parameters" :	{
		"penalty" : {
			"description":	"The norm to use in penalization: 'l1' or 'l2'.",
			"type":			"$string",
			"constraints":	{ "enum": [ "l1", "l2" ] },
			"defaultValue": "l2",
			"toolkitName":	"penalty"
		},
		"C" :	{
			"description":	"Stength of regularisation used",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue":	1,
			"toolkitName":	"C"
    	},
		"tol" :	{
			"description":	"The tolerance used in the stopping criterion",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue": 0.0001,
			"toolkitName":	"tol"
    	}
	}
}
    
