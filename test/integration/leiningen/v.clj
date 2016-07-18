(ns integration.leiningen.v
  (:import [java.nio.file Files])
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.v.impl]
            [leiningen.v.maven]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]
            [midje.config :refer [with-augmented-config]]))

(defn init!
  []
  (assert (-> (shell/sh "/bin/bash" "-c" "git init") :exit zero?)))

(defn commit0!
  []
  (assert (-> (shell/sh "/bin/bash" "-c" "git commit -m \"Commit 0\" --allow-empty") :exit zero?)))

(defn commit1!
  []
  (assert (-> (shell/sh "/bin/bash" "-c" "git commit -m \"Commit 1\" --allow-empty") :exit zero?)))

(defn tag!
  [t]
  (let [result (shell/sh "/bin/bash" "-c" (format "git tag -a --message \"Release %s\" %s" t t))]
    (assert (-> result :exit zero?) (format "Uh Oh\n: %s" result))))

(defn dirty!
  []
  (let [result (shell/sh "/bin/bash" "-c" "echo \"Hello\" >> x && git add x")]
    (assert (-> result :exit zero?) (format "Uh Oh\n: %s" result))))

(background
 (around :facts (let [tmpdir (Files/createTempDirectory
                              (.toPath (io/as-file (io/resource "tmp-git")))
                              "repo"
                              (into-array java.nio.file.attribute.FileAttribute []))]
                  (shell/with-sh-dir (str tmpdir)
                    ?form))))

(def $mproject {:version :lein-v :v {:from-scm leiningen.v.maven/from-scm
                                     :default leiningen.v.maven/default}})

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "cache task works"
  (v {:version "1.2.3" :source-paths ["/X"]} "cache") => anything
  (provided
    (spit "/X/version.clj" (as-checker (partial re-find #"1.2.3"))) => ..result..))

(fact "Raw repo returns default"
  (against-background (before :facts (do (init!))))
  (version-from-scm $mproject)
  => (contains {:version "0.0.0"
               :manifest (contains {"Implementation-Version" "0.0.0"})}))

(fact "Baseless commit returns default + commit metadata"
  (against-background (before :facts (do (init!) (commit0!))))
  (version-from-scm $mproject)
  => (contains {:version #"0.0.0-1-0x[0-9a-z]{4}"
               :manifest (contains {"Implementation-Version" #"0.0.0-1-0x[0-9a-z]{4}"})}))

(fact "Commit with positive commit distance yields complete version"
  (against-background (before :facts (do (init!) (commit0!) (tag! "v2.3.4-alpha4") (commit1!))))
  (version-from-scm $mproject)
  => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"
               :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"})}))

(fact "Dirty repo is marked in version"
  (against-background (before :facts (do (init!) (commit0!) (tag! "v2.3.4-alpha4") (commit1!) (dirty!))))
  (version-from-scm $mproject)
  => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"
               :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"})}))

(fact "update task works"
  (against-background (before :facts (do (init!) (commit0!) (tag! "v1.2.3") (commit1!))))
  (binding [leiningen.release/*level* :major-snapshot]
    (v $mproject "update")) => anything
  (version-from-scm $mproject) => (contains {:version "2.0.0-SNAPSHOT"}))

(fact "assert-anchored task works"
  (v {:workspace {:status {:tracking "" :files ()}}} "assert-anchored") => anything
  (v {:workspace {:status {:tracking "" :files ("X")}}} "assert-anchored") => (throws Exception)
  (v {:workspace {:status {:tracking "Your branch is ahead of 'origin/master' by 1 commit."
                           :files ("X")}}} "assert-anchored") => (throws Exception))

(future-fact "deploy-when-anchored hook works"
  (leiningen.deploy/deploy {}) => :x)

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
