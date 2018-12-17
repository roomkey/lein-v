(defproject com.roomkey/lein-v :lein-v
  :description "A Leiningen plugin to reflect on the SCM workspace of a project"
  :url "https://github.com/roomkey/lein-v"
  :aliases {"deploy" ["do" ["v" "abort-if-not-anchored"] "deploy"]}
  :release-tasks [["v" "assert-anchored"]
                  ["v" "update"]
                  ["vcs" "push"]
                  ["v" "abort-when-not-anchored"]
                  ["deploy" "clojars"]]
  :min-lein-version "2.0.0"
  :middleware [leiningen.v/version-from-scm
               leiningen.v/add-workspace-data]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.apache.maven/maven-artifact "3.6.0"]]
  :profiles {:dev {:dependencies [[midje "1.9.4"]]
                   :plugins [[lein-midje "3.2.1"]]
                   :eastwood {:config-files []
                              :exclude-linters []
                              :source-paths ["src"] ;; somehow "test" sneaks in anyway, so...
                              :exclude-namespaces [:test-paths]}}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Djava.io.tmpdir=./tmp"]
  :eval-in-leiningen true)
