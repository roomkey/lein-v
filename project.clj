(defproject com.roomkey/lein-v "0.0.0"
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
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.maven/maven-artifact "3.3.9"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :eastwood {:config-files []
                              :exclude-linters []
                              ;:add-linters [:unused-namespaces]
                              :source-paths ["src"] ;; somehow "test" sneaks in anyway, so...
                              :exclude-namespaces [:test-paths]}}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Djava.io.tmpdir=./tmp"]
  :repositories [["rk-public" {:url "http://rk-maven-public.s3-website-us-east-1.amazonaws.com/releases/"}]
                 ["rk-private" {:url "s3://rk-maven/releases/"}]]
  :eval-in-leiningen true)
