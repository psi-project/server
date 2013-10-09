# PSI Service Starter Kit

The [_Protocols and Structures for Inference_](http://psi.cecs.anu.edu.au) project aims to develop a general purpose web API for machine learning. This starter kit is a [Play 1](http://www.playframework.com/documentation/1.2.7)-based application that implements version 2 of the [PSI Specification](http://psi.cecs.anu.edu.au/spec). It may be used to create a new PSI service or to assist those wishing to implement completely new services.

This kit was extracted from the source for our demonstration service at <http://poseidon.cecs.anu.edu.au>. While every effort has been made to make the software robust, it is 'research' rather than 'production' code, so there are many places where it can be made more efficient and robust. It is distributed under an MIT Licence.

## Included in this kit

* A number of datasets from the UCI Machine Learning Repository that become relation resources 
n the service.
* Bindings for using Weka classifiers and clustering algorithms
* Bindings for using scikit learn (using a [companion micro-service](https://github.com/psi-project/sklearn-wrapper))

## Getting started

Requirements:

1. [Play Framework, version 1.2.7](http://www.playframework.com/download)
2. [JUnit Contrib](https://github.com/junit-team/junit.contrib) (only for testing, and specifically the Assumes extension)
3. The companion [sklearn-service](https://github.com/psi-project/sklearn-wrapper) (if using [scikit learn](http://scikit-learn.org/stable/)) 

The project also requires [Weka 3.7](http://www.cs.waikato.ac.nz/ml/weka/) and the [json-schema-validator](https://github.com/fge/json-schema-validator), but these are captured in the `depdencies.yml` file.

### Initial setup

1. Run `play dependencies` in the application's directory to have Play resolve dependencies. This will download required libraries to the `lib` directory and add a `modules` directory.

    Remaining instructions assume the app is being run in [DEV mode](http://www.playframework.com/documentation/1.2.7/main#lifecycle), which runs the service on port 9000.
2. Administration pages (which are outside the PSI specification) are secured using Play's [secure module](http://www.playframework.com/documentation/1.2.7/secure), with the username-password combination of "admin"-"admin". To change these:
    1. Run the application with `play run` and browse to <http://localhost:9000/admin/chpass>.
    2. Enter a new pass phrase and a string of random characters (remember both) to generate a new hashed password suitable for storing in the application's configuration file.
    3. In `conf/application.yml`, change `psi.admin.name` to a new name of your choosing, `psi.admin.password` to the hashed result generated above, and `psi.admin.salt` to the random string used. The changes will take effect when the service is restarted.
3. Load dataset and learner models into the database. Start the service and navigate to <http://localhost:9000/admin>. The links for loading these models trigger the system to read JSON definition files in `private/datasets` and `private/learners`. These operations may be performed again after adding new dataset or learner definitions; only new definitions will be loaded.

## Further assistance

This document will be expanded to include more help soon. See the [Play documentation](http://www.playframework.com/documentation/1.2.7) for issues relating to the web framework.
