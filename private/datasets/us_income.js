/* Internal definition of the US Income relation (UCI 'adult' dataset) and its attributes. */
{
	"name":			"us_income",
	"description":	"Low versus high income brackets as reported in 1994 US census (known as the 'Adult' data set in the UCI ML Repository)",
	"format":		"CSV";
	"path":			"/private/datasets/us_income.csv",
	"defaultAttribute": "censusData",
	"attributes":	[
		{
			"name":			"age",
			"psiType":	"attribute-definition",
			"description":	"Person's age in years",
			"attribute": "primitive://csv/1?type=integer"
		},
		{
			"name":			"workclass",
			"psiType":	"attribute-definition",
			"description":	"Employment status",
			"attribute": "primitive://csv/2?type=string&values=Private,Self-emp-not-inc,Self-emp-inc,Federal-gov,Local-gov,State-gov,Without-pay,Never-worked"
		},
		{
			"name":		"final_weight",
			"psiType":	"attribute-definition",
			"description":	"CPS-determined 'final weight'",
			"attribute": "primitive://csv/3?type=integer"
		},
		{
			"name":			"education",
			"psiType":	"attribute-definition",
			"description":	"Highest level of education",
			"attribute": "primitive://csv/4?type=string&values=Bachelors,Some-college,11th,HS-grad,Prof-school,Assoc-acdm,Assoc-voc,9th,7th-8th,12th,Masters,1st-4th,10th,Doctorate,5th-6th,Preschool"
		},
		{
			"name":			"education_years",
			"psiType":	"attribute-definition",
			"description":	"Number of years of education",
			"attribute": "primitive://csv/5?type=integer"
		},
		{
			"name":			"marital_status",
			"psiType":	"attribute-definition",
			"description":	"Marital status",
			"attribute": "primitive://csv/6?type=string&values=Married-civ-spouse,Divorced,Never-married,Separated,Widowed,Married-spouse-absent,Married-AF-spouse"
		},
		{
			"name":			"occupation",
			"psiType":	"attribute-definition",
			"description":	"Occupation",
			"attribute": "primitive://csv/7?type=string&values=Tech-support,Craft-repair,Other-service,Sales,Exec-managerial,Prof-specialty,Handlers-cleaners,Machine-op-inspct,Adm-clerical,Farming-fishing,Transport-moving,Priv-house-serv,Protective-serv,Armed-Forces"
		},
		{
			"name":			"relationship",
			"psiType":	"attribute-definition",
			"description":	"Relationship",
			"attribute": "primitive://csv/8?type=string&values=Wife,Own-child,Husband,Not-in-family,Other-relative,Unmarried"
		},
		{
			"name":			"ethnicity",
			"psiType":	"attribute-definition",
			"description":	"Ethnicity",
			"attribute": "primitive://csv/9?type=string&values=White,Asian-Pac-Islander,Amer-Indian-Eskimo,Other,Black"
		},
		{
			"name":			"sex",
			"psiType":	"attribute-definition",
			"description":	"Sex",
			"attribute": "primitive://csv/10?type=string&values=Female,Male"
		},
		{
			"name":			"capital_gain",
			"psiType":	"attribute-definition",
			"description":	"Capital gain",
			"attribute": "primitive://csv/11?type=integer"
		},
		{
			"name":			"capital_loss",
			"psiType":	"attribute-definition",
			"description":	"Capital loss",
			"attribute": "primitive://csv/12?type=integer"
		},
		{
			"name":			"hours_working",
			"psiType":	"attribute-definition",
			"description":	"Hours of work per week",
			"attribute": "primitive://csv/13?type=integer"
		},
		{
			"name":			"native_country",
			"psiType":	"attribute-definition",
			"description":	"Native country",
			"attribute": "primitive://csv/14?type=string&values=United-States,Cambodia,England,Puerto-Rico,Canada,Germany,Outlying-US(Guam-USVI-etc),India,Japan,Greece,South,China,Cuba,Iran,Honduras,Philippines,Italy,Poland,Jamaica,Vietnam,Mexico,Portugal,Ireland,France,Dominican-Republic,Laos,Ecuador,Taiwan,Haiti,Columbia,Hungary,Guatemala,Nicaragua,Scotland,Thailand,Yugoslavia,El-Salvador,Trinadad%26Tobago,Peru,Hong,Holand-Netherlands"
		},
		{
			"name":			"income",
			"psiType":	"attribute-definition",
			"description":	"Income bracket (<=50K or >50K US dollars)",
			"attribute": "primitive://csv/15?type=string&values=%3C%3D50K,%3E50K"
		},
		{
			"name":			"censusData",
			"psiType":	"attribute-definition",
			"description":	"1994 census data including income bracket (<=50K, >50K)",
			"attribute": [
				"local://localhost/data/us_income/age",
				"local://localhost/data/us_income/workclass",
				"local://localhost/data/us_income/final_weight",
				"local://localhost/data/us_income/education",
				"local://localhost/data/us_income/education_years",
				"local://localhost/data/us_income/marital_status",
				"local://localhost/data/us_income/occupation",
				"local://localhost/data/us_income/relationship",
				"local://localhost/data/us_income/ethnicity",
				"local://localhost/data/us_income/sex",
				"local://localhost/data/us_income/capital_gain",
				"local://localhost/data/us_income/capital_loss",
				"local://localhost/data/us_income/hours_working",
				"local://localhost/data/us_income/native_country",
				"local://localhost/data/us_income/income"
			]
		},
		{
			"name":			"censusDataNoIncome",
			"psiType":	"attribute-definition",
			"description":	"1994 census data excluding income bracket",
			"attribute": [
				"local://localhost/data/us_income/age",
				"local://localhost/data/us_income/workclass",
				"local://localhost/data/us_income/final_weight",
				"local://localhost/data/us_income/education",
				"local://localhost/data/us_income/education_years",
				"local://localhost/data/us_income/marital_status",
				"local://localhost/data/us_income/occupation",
				"local://localhost/data/us_income/relationship",
				"local://localhost/data/us_income/ethnicity",
				"local://localhost/data/us_income/sex",
				"local://localhost/data/us_income/capital_gain",
				"local://localhost/data/us_income/capital_loss",
				"local://localhost/data/us_income/hours_working",
				"local://localhost/data/us_income/native_country"
			]
		}
	]
}
