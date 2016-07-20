(ns integration.leiningen.v
  (:import [java.nio.file Files])
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.maven]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [utilities.git-shell :refer :all]
            [midje.sweet :refer :all]
            [midje.config :refer [with-augmented-config]]))

;; test0.repo: (shell! (init!) (commit!) (tag "v1.2.3") (commit!))
(background
 (around :facts (shell! ?form)))

(def $mproject {:version :lein-v :source-paths ["/X"] :v {:from-scm 'leiningen.v.maven/from-scm
                                                          :sign nil}})

(fact "cache task works"
  (against-background (before :facts (do (clone! "test0.repo") (commit!))))
  (v (version-from-scm $mproject) "cache") => anything
  (provided
    (spit "/X/version.clj" (as-checker (partial re-find #"1.2.3-1-0x[0-9a-f]{4,}"))) => ..result..))

(fact "update task works"
  (against-background (before :facts (do (clone! "test0.repo") (commit!))))
  (binding [leiningen.release/*level* :major-snapshot]
    (v (version-from-scm $mproject) "update")) => anything
  (version-from-scm $mproject) => (contains {:version "2.0.0-SNAPSHOT"}))

(fact "update task fails on existing commit"
  (against-background (before :facts (do (clone! "test0.repo"))))
  (binding [leiningen.release/*level* :major-snapshot]
    (v (version-from-scm $mproject) "update")) => (throws java.lang.AssertionError)
  (version-from-scm $mproject) => (contains {:version "1.2.3"}))

(fact "assert-anchored throws on dirty repo"
  (against-background (before :facts (do (clone! "test0.repo"))))
  (v (add-workspace-data $mproject) "assert-anchored") => anything
  (dirty!)
  (v (add-workspace-data $mproject) "assert-anchored") => (throws java.lang.AssertionError))

(fact "assert-anchored throws on unstable (unpushed, ahead) repo"
  (against-background (before :facts (do (clone! "test0.repo"))))
  (v (add-workspace-data $mproject) "assert-anchored") => anything
  (commit!)
  (v (add-workspace-data $mproject) "assert-anchored") => (throws java.lang.AssertionError))
