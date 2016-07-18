(ns integration.leiningen.v
  (:import [java.nio.file Files])
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.maven]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [midje.sweet :refer :all]
            [midje.config :refer [with-augmented-config]]))

(defn- sh [command] (assert (-> (shell/sh "/bin/bash" "-c" command) :exit zero?)))

(defn init! [] (sh "git init"))

(defn commit! [] (sh "git commit -m \"Commit\" --allow-empty"))

(defn tag! [t] (sh (format "git tag -a -m \"R %s\" %s" t t)))

(defn dirty! [] (sh "echo \"Hello\" >> x && git add x"))

(background
 (around :facts (let [tmpdir (Files/createTempDirectory
                              (.toPath (io/as-file (io/resource "tmp-git")))
                              "repo"
                              (into-array java.nio.file.attribute.FileAttribute []))]
                  (shell/with-sh-dir (str tmpdir)
                    ?form))))

(def $mproject {:version :lein-v :v {:from-scm leiningen.v.maven/from-scm}})

(fact "cache task works"
  (v {:version "1.2.3" :source-paths ["/X"]} "cache") => anything
  (provided
    (spit "/X/version.clj" (as-checker (partial re-find #"1.2.3"))) => ..result..))

(fact "update task works"
  (against-background (before :facts (do (init!) (commit!) (tag! "v1.2.3") (commit!))))
  (binding [leiningen.release/*level* :major-snapshot]
    (v $mproject "update")) => anything
  (version-from-scm $mproject) => (contains {:version "2.0.0-SNAPSHOT"}))

(fact "update task fails on existing commit"
  (against-background (before :facts (do (init!) (commit!) (tag! "v1.2.3"))))
  (binding [leiningen.release/*level* :major-snapshot]
    (v $mproject "update")) => (throws java.lang.AssertionError)
  (version-from-scm $mproject) => (contains {:version "1.2.3"}))

(fact "assert-anchored task works"
  (v {:workspace {:status {:tracking "" :files ()}}} "assert-anchored") => anything
  (v {:workspace {:status {:tracking "" :files ("X")}}} "assert-anchored") => (throws Exception)
  (v {:workspace {:status {:tracking "Your branch is ahead of 'origin/master' by 1 commit."
                           :files ("X")}}} "assert-anchored") => (throws Exception))
