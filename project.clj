(defproject com.roomkey/lein-v :lein-v
  :description "A Leiningen plugin to reflect on the SCM workspace of a project"
  :url "https://github.com/roomkey/lein-v"
  :release-tasks [["v" "assert-anchored"]
                  ["v" "update"]
                  ["vcs" "push"]
                  ["deploy"]]
  :min-lein-version "2.0.0"
  :hooks [leiningen.v/deploy-when-anchored]
  :middleware [leiningen.v/version-from-scm
               leiningen.v/add-workspace-data]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.apache.maven/maven-artifact "3.5.2"]]
  :profiles {:dev {:dependencies [[midje "1.9.1"]]
                   :plugins [[lein-midje "3.2.1"]]
                   :eastwood {:config-files []
                              :exclude-linters []
                              :source-paths ["src"] ;; somehow "test" sneaks in anyway, so...
                              :exclude-namespaces [:test-paths]}}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Djava.io.tmpdir=./tmp"]
  :eval-in-leiningen true)
