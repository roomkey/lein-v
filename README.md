# lein-v

Reflect on the version of a Leiningen project.

Some relevant reading:

     http://www.sonatype.com/books/mvnref-book/reference/pom-relationships-sect-pom-syntax.html
     http://semver.org/
     http://javamoods.blogspot.com/2010/10/world-of-versioning.html

## Usage

This version of lein-v is only compatible with leiningen 2.x.  There
are two lein sub-tasks within the v namespace:

lein v show
     Show the effective version of the project

lein v cache
     Cache the effective version of the project.  This also makes it
     available within the scope of the project itself via the
     version/*version* var.

Through the use of the Leiningen hooks functionality, this plugin can ensure
that the project's own view of the current version is updated before the project
is run or exported.

    (defproject my-group/my-project "1.0.0"
      :plugins [[com.roomkey/lein-v "3.0.0"]]
      ...)

In addition, it is possible to extract the version of the project directly
from the project's environment (which is currently limited to inspecting git
metadata).  The recipe:

    (use ['leiningen.v :only ['version]])
    (defproject my-group/my-project (version)
      ...)

## License

Copyright (C) 2012 Room Key

Distributed under the Eclipse Public License, the same as Clojure.
