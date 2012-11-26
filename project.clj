(defproject com.roomkey/lein-v "3.1.0"
  :description "A Leiningen plugin to reflect on the version of a project"
  :url "https://github.com/g1nn13/lein-v"
  :plugins [[s3-wagon-private "1.1.2"]]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["releases" {:url "s3p://rk-maven/releases/"}]]
  :eval-in-leiningen true)
