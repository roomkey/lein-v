(ns leiningen.test.v
  (:require [leiningen.v.git :as git]
            [leiningen.deploy]
            [leiningen.v :refer :all]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]))

(fact "git version is injected if available"
  (version-from-scm {}) => (contains {:version "1.2.3-3-0xabcd"})
  (provided (git/version) => ["1.2.3" 3 "abcd" true])
  (version-from-scm {}) => (contains {:version "1.2.3"})
  (provided (git/version) => ["1.2.3" 0 "abcd" true]))

(fact "default version is returned if git version is not available"
  (version-from-scm {:version :lein-v}) => (contains {:version "0.0.1-SNAPSHOT"})
  (provided (git/version) => nil))

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
