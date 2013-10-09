{
	"name": "forest_fires",
	"description": "The burned area of forest fires in Montesinho park in the northeast region of Portugal, and associated meteorological and other data. See http://archive.ics.uci.edu/ml/datasets/Forest+Fires",
	"format": "CSV",
	"path": "/private/datasets/forest_fires.csv",
	"defaultAttribute": "featuresAndArea",
	"attributes": [
		{
			"psiType": "attribute-definition",
			"name": "burnedArea",
			"attribute": "primitive://csv/13?type=number&description=the%20burned%20area%20of%20the%20forest%20(in%20ha):%200.00%20to%201090.84%20(very%20skewed%20towards%200.0)"
		},
		{
			"psiType": "attribute-definition",
			"name": "featuresAndArea",
			"description": "Feature vector of meteorological and other data at time of fire, plus burned area",
			"attribute": [
				"primitive://csv/1?type=integer&values=1,2,3,4,5,6,7,8,9&description=x-axis%20spatial%20coordinate%20within%20the%20Montesinho%20park%20map:%201%20to%209",
				"primitive://csv/2?type=integer&values=2,3,4,5,6,7,8,9&description=y-axis%20spatial%20coordinate%20within%20the%20Montesinho%20park%20map:%202%20to%209",
				"primitive://csv/3?type=string&values=jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec&description=month%20of%20the%20year:%20'jan'%20to%20'dec'%20",
				"primitive://csv/4?type=string&values=mon,tue,wed,thu,fri,sat,sun&description=day%20of%20the%20week:%20'mon'%20to%20'sun'",
				"primitive://csv/5?type=number&description=FFMC%20index%20from%20the%20FWI%20system:%2018.7%20to%2096.20",
				"primitive://csv/6?type=number&description=DMC%20index%20from%20the%20FWI%20system:%201.1%20to%20291.3",
				"primitive://csv/7?type=number&description=DC%20index%20from%20the%20FWI%20system:%207.9%20to%20860.6",
				"primitive://csv/8?type=number&description=ISI%20index%20from%20the%20FWI%20system:%200.0%20to%2056.1",
				"primitive://csv/9?type=number&description=temperature%20in%20Celsius%20degrees:%202.2%20to%2033.3",
				"primitive://csv/10?type=number&description=relative%20humidity%20in%20%25:%2015.0%20to%20100",
				"primitive://csv/11?type=number&description=wind%20speed%20in%20km/h:%200.4%20to%209.4",
				"primitive://csv/12?type=number&description=outside%20rain%20in%20mm/m2%20:%200.0%20to%206.4",
				"local://localhost/data/forest_fires/burnedArea"
			]
		},
		{
			"psiType": "attribute-definition",
			"name": "features",
			"description": "Feature vector of meteorological and other data at time of fire",
			"attribute": [
			  	"local://localhost/data/forest_fires/featuresAndArea/1",
			  	"local://localhost/data/forest_fires/featuresAndArea/2",
			  	"local://localhost/data/forest_fires/featuresAndArea/3",
			  	"local://localhost/data/forest_fires/featuresAndArea/4",
			  	"local://localhost/data/forest_fires/featuresAndArea/5",
			  	"local://localhost/data/forest_fires/featuresAndArea/6",
			  	"local://localhost/data/forest_fires/featuresAndArea/7",
			  	"local://localhost/data/forest_fires/featuresAndArea/8",
			  	"local://localhost/data/forest_fires/featuresAndArea/9",
			  	"local://localhost/data/forest_fires/featuresAndArea/10",
			  	"local://localhost/data/forest_fires/featuresAndArea/11",
			  	"local://localhost/data/forest_fires/featuresAndArea/12"
			]
		}
	]
}