# lein-v #

Drive leiningen project version from git instead of the other way around.

## Motivation ##
The lein-v plugin was driven by several beliefs:

	1. Versioning should be painless in the simplest cases
	2. Unique (and reproducible/commited) source should produce unique versions
	3. Versioning information should live in the SCM repo -the source of source truth
	4. Version information is metadata and should not be stored within the data it describes

Lein-v uses git metadata to build a unique, reproducible and semantically meaningful version for every commit.  Along the way, it adds useful metadata to your project and artifacts (jar and war files) to tie them back to a specific commit.  Finally, it helps ensure that you never release an irreproduceable artifact.

## Task Usage ##

There are two lein sub-tasks within the v namespace intended for direct use:

### lein v show
Show the effective version of the project and workspace state.

### lein v cache
Cache the effective version of the project to a file (default is `version.clj`) in the first source directory (typically `src`).  It is possible to have the version cached to a file automatically by defining a prep task in your project:

    :prep-tasks [["v" "cache" "src"]]

## Hooks ##
Through the use of the Leiningen hooks functionality, lein-v ensures that
leiningen's own view of the current version is updated before tasks are run. Thus this

    (defproject my-group/my-project :lein-v
	  :plugins [[com.roomkey/lein-v "5.0.0"]]
      ...)

becomes this:

    (defproject my-group/my-project "1.0.1-2-0xabcd"
      :plugins [[com.roomkey/lein-v "5.0.0"]]
      ...)

Assuming that there is a git tag `v1.0.1` on the commit `HEAD~~`, and that the SHA of `HEAD` is uniquely identified by `abcd`.  This behavior is automatically enabled whenever lein-v finds the project version to be the keyword `:lein-v`.

## Dependencies

In case you're using a monorepository, you could also use lein-v to determine the current version of dependencies.

Add the `leiningen.v/dependency-version-from-scm` middleware to your project like this:

```
  :middleware [leiningen.v/version-from-scm
               leiningen.v/dependency-version-from-scm
               leiningen.v/add-workspace-data]
```

Now, if you set the version of a dependency from your monorepo to nil (just as you would using managed-dependencies), it will be replaced
with the current version from git (which is the same as the version of the project you're currently working on).

```
  :dependencies
  [[commons-io "2.5"]
   [example/lib-a nil]
   [example/lib-b nil]
   [org.clojure/clojure "1.8.0"]])
```

becomes

```
  :dependencies
  [[commons-io "2.5"]
   [example/lib-a "1.0.1-2-0xabcd"]
   [example/lib-b "1.0.1-2-0xabcd"]
   [org.clojure/clojure "1.8.0"]])
```


## Support for lein release ##
As of version 5.0, lein-v adds support for leiningen's `release` task.  Specifically, the `lein v update` task can anchor a release process that ensures that git tags are created and pushed, and that those tags conform to sane versioning expectations.  To use `lein release` with lein-v, first modify `project.clj` (or your leiningen user profile) as follows:

    :release-tasks [["vcs" "assert-committed"]
                    ["v" "update"] ;; compute new version & tag it
                    ["vcs" "push"]
                    ["deploy"]]

To effect version changes, lein-v's `update-version` task sees the versioning parameter
provided to lein release and operates as follows:

|current       |directive         |result           |
|:-------------|:-----------------|:----------------|
|1.0.2         |`:major`          |2.0.0            |
|1.1.4         |`:minor`          |1.2.0            |
|2.5.6         |`:patch`          |2.5.7            |

In addition to incrementing the standard numeric version components, you can qualify
any of the above directives with typical qualifiers like `alpha`, `beta`, `rc`.  For example:

|current       |directive         |result           |
|--------------|------------------|-----------------|
|1.0.2         |`:minor-alpha`    |1.1.0-alpha      |
|4.2.8         |`:major-rc`       |5.0.0-RC         |

When the current version is a qualified version, you can increment the current qualifier,
advance to the next qualifier or simply release an unqualified version.  Here are some examples:

|current       |directive         |result           |
|--------------|------------------|-----------------|
|1.0.2-alpha   |`:alpha`          |1.0.2-alpha2     |
|1.0.2-alpha   |`:beta`           |1.0.2-beta       |
|1.0.2-beta    |`:rc`             |1.0.2-rc         |
|1.0.2-rc      |`:rc`             |1.0.2-rc2        |
|3.2.0-rc2     |`:release`        |3.2.0            |

Snapshot versions are similar, but the resulting version is never changed.

|current       |directive         |result           |
|--------------|------------------|-----------------|
|3.2.0         |`:minor-snapshot` |3.3.0-SNAPSHOT   |
|3.3.0-SNAPSHOT|`:snapshot`       |3.3.0-SNAPSHOT   |
|3.3.0-SNAPSHOT|`:release`        |3.3.0            |

Finally, lein-v enforces some common-sense rules:

* For commits without a version tag, the base version will be extended with a build number using the commit distance from
HEAD to the most recent version tag (looking towards the root of the tree) and the unique SHA prefix of the commit.
* You can never go backwards with versions.  This includes qualifiers, which are orderd as follows:
  1. `alpha`
  2. `beta`
  3. `rc`
  4. `snapshot`
* When a git repo is first used with lein-v and has no version tags, the default base version is 0.0.0, and it
  is reported with a distance from the root commit and the relevant SHA (0.0.0-23-0xabcd).
* When tags are created in the git repo, they are prefixed with the letter 'v'.

It is still possible to do a raw `lein deploy`, in which case the version will be that determined by
lein-v (most likely something like "1.0.1-2-0xabcd").

Note: you can provide your own implementation of many of these rules.  See the source code for details on defining data types adhering to the protocols in the `leiningen.v.protocols` namespace.  Currently there are implementations for maven (version 3) and Semantic Versioning (version 2) available.

### References and Relevant Reading ###

* (<http://maven.apache.org/ref/3.2.5/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html>)
* (<http://www.sonatype.com/books/mvnref-book/reference/pom-relationships-sect-pom-syntax.html>)
* (<http://semver.org/>)
* (<http://javamoods.blogspot.com/2010/10/world-of-versioning.html>)
* (<http://download.eclipse.org/aether/aether-core/1.0.1/apidocs/org/eclipse/aether/util/version/GenericVersionScheme.html>)
* (<http://git.eclipse.org/c/aether/aether-core.git/tree/aether-util/src/main/java/org/eclipse/aether/util/version/GenericVersion.java>)
* (<http://semver.org/>)
* (<http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html#pom-reationships-sect-versions>)
* (<http://mojo.codehaus.org/versions-maven-plugin/version-rules.html>)
* (<http://maven.40175.n5.nabble.com/How-to-use-alternative-version-numbering-scheme-td123806.html>)
* (<http://maven.apache.org/ref/3.2.5/maven-artifact/index.html>)
* (<https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning>)
* (<http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution>)
* (<http://dev.clojure.org/display/doc/Maven+Settings+and+Repositories>)
* (<http://maven.40175.n5.nabble.com/How-to-use-SNAPSHOT-feature-together-with-BETA-qualifier-td73263.html>)

## License ##

Copyright (C) 2016 Room Key

Distributed under the Eclipse Public License, the same as Clojure.
