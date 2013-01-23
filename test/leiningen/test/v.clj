(ns leiningen.test.v
  (:require [leiningen.v.git :as v-git]
            [leiningen.v.file :as v-file]
            [leiningen.deploy])
  (:use [leiningen.v]
        [clojure.test]
        [midje.sweet]))

(fact "git version is returned if available"
  (version ..project..) => "1.0.0"
  (provided (v-git/version ..project..) => "1.0.0")
  (version ..project..) => "1.0.0-1"
  (provided (v-git/version ..project..) => "1.0.0-1"))

(fact "file version is returned if git is unavailable"
  (version ..project..) => "1.0.0"
  (provided (v-file/version ..project..) => "1.0.0"
            (v-git/version ..project..) => nil))

(fact "default version is returned if neither git nor cache file is unavailable"
  (version ..project..) => "unknown"
  (provided (v-file/version ..project..) => nil
            (v-git/version ..project..) => nil))

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
