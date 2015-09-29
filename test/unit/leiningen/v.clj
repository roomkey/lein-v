(ns unit.leiningen.v
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "git version is injected if available"
  (version-from-scm {}) => (contains {:version "1.2.3-3-0xabcd"})
  (provided (git/version) => ["1.2.3" 3 "abcd" false])
  (version-from-scm {}) => (contains {:version "1.2.3"})
  (provided (git/version) => ["1.2.3" 0 "abcd" false])
  (version-from-scm {}) => (contains {:version "1.2.3-SNAPSHOT"})
  (provided (git/version) => ["1.2.3-SNAPSHOT" 4 "8888" false]))

(fact "default version is returned if git version is not available"
  (version-from-scm {:version :lein-v}) => (contains {:version "0.0.0-0-0xabcd"})
  (provided (git/version) => nil
            (git/sha) => "abcd"))

(fact "dirty version marker is returned if repo is dirty"
  (version-from-scm {:version :lein-v}) => (contains {:version "DIRTY"})
  (provided (git/version) => ["1.2.3" 4 "8888" true]))

(fact "tag is created with updated version"
  (update {} :minor) => (as-string "1.3.0")
  (provided
    (git/version) => ["1.2.3" 3 "abcd" false]
    (git/tag "1.3.0") => ..tagResult..))

(fact "tag is not created when version does not change"
  (update {} :snapshot) => (as-string "1.2.3-SNAPSHOT")
  (provided
    (git/version) => ["1.2.3-SNAPSHOT" 3 "abcd" false]
    (git/tag anything) => ..tagResult.. :times 0))

(fact "compound operation is correctly parsed"
  (update {} :minor-alpha) => (as-string "1.3.0-alpha")
  (provided
    (git/version) => ["1.2.3" 3 "abcd" false]
    (git/tag "1.3.0-alpha") => ..tagResult..))

(fact "deploy-when-anchored ensures deploy tasks are called when project is on a stable commit and clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files []}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => ..something..
  (provided (leiningen.deploy/deploy ..project..) => ..something..))

(fact "deploy-when-anchored ensures deploy tasks are not called when project is not on a stable commit"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master...origin/master [ahead 1]"]
                                                                    :files []}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => anything
  (provided (leiningen.deploy/deploy ..project..) => ..anything.. :times 0))

(fact "deploy-when-anchored ensures deploy tasks are not called when project is not clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files ["?? new-file.txt"]}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => anything
  (provided (leiningen.deploy/deploy ..project..) => ..anything.. :times 0))
