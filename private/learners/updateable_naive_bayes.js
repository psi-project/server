{
	"name" :		"updateable_naive_bayes",
	"description" :	"Updateable Naive Bayes",
	"implementation" :	"weka.classifiers.bayes.NaiveBayesUpdateable",
	"learnerModelClass" : "models.learner.WekaLearner",
	"resources" :	{
		"/source" :		{ "$arrayAttribute" : { "allItems": "$atomicValueSchema", "description": "Instance details that will be used to predict the value of target" } },
		"/target" :		{ "$nominalAttribute" : { "allItems" : "$string", "description": "The label or class of each instance (i.e., what will be predicted)" } },
		"?weight" :		{ "$numberAttribute" : { "description" : "The weight or amount of attention to give an instance during training" } }
	}, 
	"parameters" :	{
		"use_kernel_estimator" : {
			"description":	"Use kernel estimation for modelling numeric attributes rather than a single normal distribution",
			"toolkitName":	"-K"
		}
	}
}
    
