(ns integration.lein-v.plugin
  (:import [java.nio.file Files])
  (:refer-clojure :exclude [update])
  (:require [lein-v.plugin :refer [middleware hooks]]
            [leiningen.v.maven]
            [leiningen.deploy]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [midje.sweet :refer :all]))

(defn- sh [command] (assert (-> (shell/sh "/bin/bash" "-c" command) :exit zero?)))

(defn init! [] (sh "git init"))

(defn commit! [] (sh "git commit -m \"Commit\" --allow-empty"))

(defn tag! [t] (sh (format "git tag -a -m \"R %s\" %s" t t)))

(defn dirty! [] (sh "echo \"Hello\" >> x && git add x"))

;; simple.repo: (do (init!) (commit!) (tag "v1.2.3"))
;; Create a bundle with: `git bundle create ../simple.repo --all`
(defn clone! [bundle] (sh (format "git clone %s . -b master" (-> bundle io/resource io/as-file str))))

(background
 (around :facts (let [tmpdir (Files/createTempDirectory
                              (.toPath (io/as-file (io/resource "tmp-git")))
                              "repo"
                              (into-array java.nio.file.attribute.FileAttribute []))]
                  (shell/with-sh-dir (str tmpdir)
                    ?form))))

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
  (against-background (before :facts (do (clone! "test-2.repo"))))
  (middleware $mproject)
  => (contains {:version "2.3.4-alpha4"
               :manifest (contains {"Implementation-Version" "2.3.4-alpha4"})}))

(fact "Commit with positive commit distance yields complete version"
  (against-background (before :facts (do (clone! "test-2.repo") (commit!))))
  (middleware $mproject)
  => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"
               :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}"})}))

(fact "Dirty repo is marked in version"
  (against-background (before :facts (do (clone! "test-2.repo") (commit!) (dirty!))))
  (middleware $mproject)
  => (contains {:version #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"
               :manifest (contains {"Implementation-Version" #"2.3.4-alpha4-1-0x[0-9a-f]{4,}-DIRTY"})}))

(fact "Workspace data is injected"
  (against-background (before :facts (do (clone! "test-2.repo") (commit!) (dirty!))))
  (middleware $mproject)
  => (contains {:workspace (just {:describe string? :status map?})
               :manifest (contains {"Workspace-Description" #"v2.3.4-alpha4-1-g[0-9a-f]{4,}-DIRTY"
                                    "Workspace-Tracking-Status" #"## master.*"
                                    "Workspace-File-Status" "A  x"})}))

(fact "deploy-when-anchored hook works"
  (against-background (before :facts (do (clone! "test-2.repo") (commit!) (dirty!))))
  (hooks) => anything
  (leiningen.deploy/deploy (middleware $mproject)) => nil)
