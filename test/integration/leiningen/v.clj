(ns integration.leiningen.v
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]
            [midje.config :refer [with-augmented-config]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "cache task works"
  (v {:version "1.2.3" :source-paths ["/X"]} "cache") => anything
  (provided
    (spit "/X/version.clj" (as-checker (partial re-find #"1.2.3"))) => ..result..))

(midje.config/with-augmented-config {:partial-prerequisites true}
  (fact "update task works"
    (binding [leiningen.release/*level* :major-snapshot] (v {} "update")) => anything
    (provided
      (#'git/git-command (as-checker (partial re-find #"describe.*"))) => (list "v1.2.3-4-g56789")
      (#'git/git-command (as-checker (partial re-find #"tag.*v2.0.0-SNAPSHOT"))) => ..result..)))

(fact "assert-anchored task works"
  (v {:workspace {:status {:tracking "" :files ()}}} "assert-anchored") => anything
  (v {:workspace {:status {:tracking "" :files ("X")}}} "assert-anchored") => (throws Exception)
  (v {:workspace {:status {:tracking "Your branch is ahead of 'origin/master' by 1 commit."
                           :files ("X")}}} "assert-anchored") => (throws Exception))

(future-fact "deploy-when-anchored hook works"
             (leiningen.deploy/deploy {}) => :x)

(fact "version-from-scm middleware works"
  (binding [leiningen.release/*level* :major-snapshot]
    (version-from-scm {})) => (contains {:version "1.2.3"
                                         :manifest (contains {"Implementation-Version" "1.2.3"})})
    (provided
      (#'git/git-command (as-checker (partial re-find #"describe.*"))) => (list "v1.2.3-0-g56789")))

(fact "add-workspace-data middleware works"
  (let [describe0 "v1.2.3-4-g5678"
        status0 "## master"
        status1 "M TODO.txt"
        status2 "M project.clj"]
    (add-workspace-data {}) => (contains {:workspace (just {:describe describe0
                                                            :status map?})
                                          :manifest (contains {"Workspace-Description" describe0
                                                               "Workspace-Tracking-Status" status0
                                                               "Workspace-File-Status"
                                                               (str status1 " || " status2)})})
   (provided
     (#'git/git-command (as-checker (partial re-find #"describe.*"))) => (list describe0)
     (#'git/git-command (as-checker (partial re-find #"status.*"))) => (list status0 status1 status2))))
