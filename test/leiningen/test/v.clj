(ns leiningen.test.v
  (:require [leiningen.v.git :as v-git]
            [leiningen.v.file :as v-file]
            [leiningen.deploy])
  (:use [leiningen.v]
        [clojure.test]
        [midje.sweet]))

(fact "SCM version is injected if available"
  (version-from-scm {}) => (contains {:version ..gitVersion..})
  (provided (v-git/version (as-checker map?)) => ..gitVersion..))

(fact "file version is returned if git is unavailable"
  (version-from-scm {}) => (contains {:version ..fileVersion..})
  (provided (v-file/version (as-checker map?)) => ..fileVersion..
            (v-git/version (as-checker map?)) => nil))

(fact "default version is returned if neither git nor cache file is unavailable"
  (version-from-scm {:version ..version..}) => (contains {:version ..version..})
  (provided (v-file/version (as-checker map?)) => nil
            (v-git/version (as-checker map?)) => nil))

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
