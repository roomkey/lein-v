(ns integration.lein-v.plugin
  (:import [java.nio.file Files])
  (:refer-clojure :exclude [update])
  (:require [lein-v.plugin :refer [middleware hooks]]
            [leiningen.v.maven]
            [leiningen.deploy]
            [clojure.java.io :as io]
            [utilities.git-shell :refer :all]
            [midje.sweet :refer :all]))

;; test1.repo: (shell! (init!) (commit!) (tag "v2.3.4-alpha4"))

(background (around :facts (binding [leiningen.core.main/*exit-process?* false] (shell! ?form))))

(def $mproject {:version :lein-v :v {:from-scm 'leiningen.v.maven/from-scm}})

(fact "Raw repo returns default"
      (against-background (before :facts (do (init!))))
      (middleware $mproject)
      => (contains {:version "0.0.0"
                    :manifest (contains {"Implementation-Version" "0.0.0"})}))

(fact "Baseless commit returns default + commit metadata"
      (against-background (before :facts (do (init!) (commit!))))
      (middleware $mproject)
      => (contains {:version #"0.0.0-1-0x[0-9a-z]{4}"
                    :manifest (contains {"Implementation-Version" #"0.0.0-1-0x[0-9a-z]{4}"})}))

(fact "Commit with zero commit distance yields simple version"
      (against-background (before :facts (do (clone! "test1.repo"))))
      (middleware $mproject)
      => (contains {:version "2.3.4-alpha4"
                    :manifest (contains {"Implementation-Version" "2.3.4-alpha4"})}))

(fact "Commit with zero commit distance yields simple version on different manifest entry key"
      (against-background (before :facts (do (clone! "test1.repo"))))
      (middleware (assoc-in $mproject [:v :manifest-version-name] "Specification-Version"))
      => (contains {:version "2.3.4-alpha4"
                    :manifest (contains {"Specification-Version" "2.3.4-alpha4"})}))

(fact "Commit with positive commit distance yields complete version"
      (against-background (before :facts (do (clone! "test1.repo") (commit!))))
      (middleware $mproject)
      => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"
                    :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"})}))

(fact "Dirty repo is marked in version"
      (against-background (before :facts (do (clone! "test1.repo") (commit!) (dirty!))))
      (middleware $mproject)
      => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"
                    :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"})}))

(fact "Workspace data is injected"
      (against-background (before :facts (do (clone! "test1.repo") (commit!) (dirty!))))
      (middleware $mproject)
      => (contains {:workspace (just {:describe string? :status map?})
                    :manifest (contains {"Workspace-Description" #"v2.3.4-alpha4-1-g[0-9a-f]{4,}-DIRTY"
                                         "Workspace-Tracking-Status" #"## master.*"
                                         "Workspace-File-Status" "A  x"})}))

(fact "deploy-when-anchored hook works"
      (against-background (before :facts (do (clone! "test1.repo") (commit!) (dirty!))))
      (hooks) => anything
      (leiningen.deploy/deploy (middleware $mproject)) => (throws clojure.lang.ExceptionInfo #"Workspace is not anchored"))
