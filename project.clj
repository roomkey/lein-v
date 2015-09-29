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
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :eastwood {:config-files []
                              :exclude-linters []
                              ;:add-linters [:unused-namespaces]
                              :source-paths ["src"] ;; somehow "test" sneaks in anyway, so...
                              :exclude-namespaces [:test-paths]}}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["rk-public" {:url "http://rk-maven-public.s3-website-us-east-1.amazonaws.com/releases/"}]
                 ["releases" {:url "s3://rk-maven-public/releases/"}]
                 ["rk-private" {:url "s3://rk-maven/releases/"}]]
  :eval-in-leiningen true)
