(defproject com.roomkey/lein-v :lein-v
  :description "A Leiningen plugin to reflect on the SCM workspace of a project"
  :url "https://github.com/roomkey/lein-v"
  :plugins [[lein-maven-s3-wagon "0.2.4"]]
  :min-lein-version "2.0.0"
  :middleware [leiningen.v/version-from-scm]
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["rk-public" {:url "http://rk-maven-public.s3-website-us-east-1.amazonaws.com/releases/"}]
                 ["releases" {:url "s3://rk-maven-public/releases/"}]
                 ["rk-private" {:url "s3://rk-maven/releases/"}]]
  :eval-in-leiningen true)
