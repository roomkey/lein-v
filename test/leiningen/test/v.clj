(ns leiningen.test.v
  (:require [leiningen.v.git :as v-git]
            [leiningen.v.file :as v-file])
  (:use [leiningen.v]
        [clojure.test]
        [midje.sweet]))

(fact "git version is returned if available"
  (version ..project..) => "1.0.0"
  (provided (v-git/version) => "1.0.0")
  (version ..project..) => "1.0.0-1"
  (provided (v-git/version) => "1.0.0-1"))

(fact "file version is returned if git is unavailable"
  (version ..project..) => "1.0.0"
  (provided (v-file/version) => "1.0.0"
            (v-git/version) => nil))

(fact "default version is returned if neither git nor cache file is unavailable"
  (version ..project..) => "unknown"
  (provided (v-file/version) => nil
            (v-git/version) => nil))

(defn tripwire [& _])

(fact "when-anchored-hook calls task when project is on a stable commit and clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files []}}})
  (when-anchored-hook tripwire ..project..) => ..something..
  (provided (tripwire ..project..) => ..something..))

(fact "when-anchored-hook does not call task when project is not on a stable commit"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master...origin/master [ahead 1]"]
                                                                    :files []}}})
  (when-anchored-hook tripwire ..project..) => anything
  (provided (tripwire ..project..) => ..anything.. :times 0))

(fact "when-anchored-hook does not call task when project is not clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files ["?? new-file.txt"]}}})
  (when-anchored-hook tripwire ..project..) => anything
  (provided (tripwire ..project..) => ..anything.. :times 0))
