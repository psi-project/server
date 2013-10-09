{
	"name" :		"j48",
	"description" :	"J48",
	"implementation" :	"weka.classifiers.trees.J48",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"unpruned" : {
			"description":	"Use unpruned tree",
			"toolkitName":	"-U"
		},
		"confidence" :	{
			"description":	"Set confidence threshold for pruning",
			"type":			"$number",
			"constraints":	{ "min" : 0 },
			"defaultValue":	0.25,
			"toolkitName":	"-C"
    	},
    	"min_instances" :	{
    		"description":	"Set minimum number of instances per leaf",
    		"type":			"$integer",
    		"constraints":	{ "min" : 1 },
    		"defaultValue":	2,
    		"toolkitName":	"-M"
    	},
    	"reduced_error_pruning" :	{
    		"description":	"Use reduced error pruning. No subtree raising is performed.",
    		"toolkitName":	"-R"
    	},
    	"num_folds" :	{
    		"description" :	"Set number of folds for reduced error pruning. One fold is used as the pruning set",
    		"type":			"$integer",
    		"constraints":	{ "min": 1 },
    		"defaultValue":	3,
    		"toolkitName":	"-N"
    	},
    	"binary_splits":	{
    		"description":	"Use binary splits for nominal attributes",
			"toolkitName":	"-B"
		},
		"no_subtree_raising" :	{
			"description":	"Don't perform subtree raising",
			"toolkitName":	"-S"
		},
		"no_clean_up" :	{
			"description":	"Do not clean up after the tree has been built",
			"toolkitName":	"-L"
		},
		"laplace_smoothing" :	{
			"description":	"Use Laplace smoothing for predicted probabilities",
			"toolkitName":	"-A"
		},
		"seed" :	{
			"description":	"Seed for random data shuffling",
			"type":			"$integer",
			"defaultValue":	1,
			"toolkitName":	"-Q"
		}
	}
}
    
