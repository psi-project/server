{
	"name" :		"naive_bayes",
	"description" :	"Naive Bayes",
	"implementation" :	"weka.classifiers.bayes.NaiveBayes",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"use_kernel_estimator" : {
			"description":	"Use kernel density estimator rather than normal distribution for numeric attributes",
			"toolkitName":	"-K"
		},
		"use_discretization" :	{
			"description":	"Use supervised discretization to process numeric attributes",
			"toolkitName":	"-D"
    	}
	}
}
    
