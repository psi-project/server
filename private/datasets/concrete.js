{
	"name": "concrete",
	"description": "Concrete compressive strength given mixture properties and age. See http://archive.ics.uci.edu/ml/datasets/Concrete+Compressive+Strength",
	"format": "CSV",
	"path": "/private/datasets/concrete.csv",
	"defaultAttribute": "featuresAndStrength",
	"attributes": [
		{
			"psiType": "attribute-definition",
			"name": "compressiveStrength",
			"attribute": "primitive://csv/9?type=number&description=Concrete%20compressive%20strength%20--%20MPa"
		},
		{
			"psiType": "attribute-definition",
			"name": "featuresAndStrength",
			"description": "Concrete mixture properties, age of pour and its compressive strength",
			"attribute": [
	              "primitive://csv/1?type=number&description=Cement%20(component%201)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/2?type=number&description=Blast%20Furnace%20Slag%20(component%202)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/3?type=number&description=Fly%20Ash%20(component%203)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/4?type=number&description=Water%20(component%204)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/5?type=number&description=Superplasticizer%20(component%205)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/6?type=number&description=Coarse%20Aggregate%20(component%206)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/7?type=number&description=Fine%20Aggregate%20(component%207)%20--%20kg%20in%20a%20m3%20mixture",
	              "primitive://csv/8?type=integer&description=Age%20--%20Day%20(1~365)",
	              "local://localhost/data/concrete/compressiveStrength"
	        ]
		},
		{
			"psiType": "attribute-definition",
			"name": "features",
			"description": "Concrete mixture properties and age of pour",
			"attribute": [
			  	"local://localhost/data/concrete/featuresAndStrength/1",
			  	"local://localhost/data/concrete/featuresAndStrength/2",
			  	"local://localhost/data/concrete/featuresAndStrength/3",
			  	"local://localhost/data/concrete/featuresAndStrength/4",
			  	"local://localhost/data/concrete/featuresAndStrength/5",
			  	"local://localhost/data/concrete/featuresAndStrength/6",
			  	"local://localhost/data/concrete/featuresAndStrength/7",
			  	"local://localhost/data/concrete/featuresAndStrength/8"
			]
		}
	]
}