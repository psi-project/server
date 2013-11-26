/* Internal definition of a white wine quality relation (winequality in UCI repo) and its attributes. */
{
	"name":			"white_wine",
	"description":	"Physicochemical data describing white wine (from the Vinho Verde region) and expert ratings of its quality",
	"format":		"CSV",
	"path":			"/private/datasets/winequality_white.csv",
	"defaultAttribute": "wineDetails",
	"attributes":	[
		{
			"name":			"fixed_acidity",
			"psiType":	"attribute-definition",
			"description":	"Fixed acidity",
			"attribute": "primitive://csv/1?type=number"
		},
		{
			"name":			"volatile_acidity",
			"psiType":	"attribute-definition",
			"description":	"Volatile acidity",
			"attribute": "primitive://csv/2?type=number"
		},
		{
			"name":			"citric_acid",
			"psiType":	"attribute-definition",
			"description":	"Citric acid",
			"attribute": "primitive://csv/3?type=number"
		},
		{
			"name":			"residual_sugar",
			"psiType":	"attribute-definition",
			"description":	"Residual sugar",
			"attribute": "primitive://csv/4?type=number"
		},
		{
			"name":			"chlorides",
			"psiType":	"attribute-definition",
			"description":	"Chlorides",
			"attribute": "primitive://csv/5?type=number"
		},
		{
			"name":			"free_SO2",
			"psiType":	"attribute-definition",
			"description":	"Free sulfur dioxide",
			"attribute": "primitive://csv/6?type=number"
		},
		{
			"name":			"total_SO2",
			"psiType":	"attribute-definition",
			"description":	"Total sulfur dioxide",
			"attribute": "primitive://csv/7?type=number"
		},
		{
			"name":			"density",
			"psiType":	"attribute-definition",
			"description":	"Density",
			"attribute": "primitive://csv/8?type=number"
		},
		{
			"name":			"pH",
			"psiType":	"attribute-definition",
			"description":	"pH",
			"attribute": "primitive://csv/9?type=number"
		},
		{
			"name":			"sulphates",
			"psiType":	"attribute-definition",
			"description":	"Sulphates",
			"attribute": "primitive://csv/10?type=number"
		},
		{
			"name":			"alcohol",
			"psiType":	"attribute-definition",
			"description":	"Alchohol",
			"attribute": "primitive://csv/11?type=number"
		},
		{
			"name":			"quality",
			"psiType":	"attribute-definition",
			"description":	"The quality of the wine as a number between 0 (worst) and 10 (best)",
			"attribute": "primitive://csv/12?type=number"
		},
		{
			"name":			"qualityClass",
			"psiType":	"attribute-definition",
			"description":	"The quality of the wine as a nominal category between 0 (worst) and 10 (best)",
			"attribute": "primitive://csv/12?type=string&values=0,1,2,3,4,5,6,7,8,9,10"
		},
		{
			"name":			"wineDetails",
			"psiType":	"attribute-definition",
			"description":	"Wine physicochemical properties and expert quality rating (as a number 1-10 and as a nominal class)",
			"attribute": {
				"fixed_acidity": "local://localhost/data/white_wine/fixed_acidity",
				"volatile_acidity": "local://localhost/data/white_wine/volatile_acidity",
				"citric_acid": "local://localhost/data/white_wine/citric_acid",
				"residual_sugar": "local://localhost/data/white_wine/residual_sugar",
				"chlorides": "local://localhost/data/white_wine/chlorides",
				"free_SO2": "local://localhost/data/white_wine/free_SO2",
				"total_SO2": "local://localhost/data/white_wine/total_SO2",
				"density": "local://localhost/data/white_wine/density",
				"pH": "local://localhost/data/white_wine/pH",
				"sulphates": "local://localhost/data/white_wine/sulphates",
				"alcohol": "local://localhost/data/white_wine/alcohol",
				"quality": "local://localhost/data/white_wine/quality",
				"qualityClass": "local://localhost/data/white_wine/qualityClass"
			}
		},
		{
			"name":			"physicochemicalData",
			"psiType":	"attribute-definition",
			"description":	"Wine physicochemical properties as feature vector",
			"attribute": [
				"local://localhost/data/white_wine/fixed_acidity",
				"local://localhost/data/white_wine/volatile_acidity",
				"local://localhost/data/white_wine/citric_acid",
				"local://localhost/data/white_wine/residual_sugar",
				"local://localhost/data/white_wine/chlorides",
				"local://localhost/data/white_wine/free_SO2",
				"local://localhost/data/white_wine/total_SO2",
				"local://localhost/data/white_wine/density",
				"local://localhost/data/white_wine/pH",
				"local://localhost/data/white_wine/sulphates",
				"local://localhost/data/white_wine/alcohol"
			]
		}
	]
}
