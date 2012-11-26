(ns leiningen.test.v
  (:require [leiningen.v.git :as v-git]
            [leiningen.v.file :as v-file])
  (:use [leiningen.v]
        [clojure.test]
        [midje.sweet]))

(fact "git version is returned if available"
  (version) => "1.0.0"
  (provided (v-git/version) => "1.0.0"))

(fact "git version with build is respected"
  (version) => "1.0.0-1"
  (provided (v-git/git-describe) => "v1.0.0-1-gb2e2"))

(fact "file version is returned if git is unavailable"
  (version) => "1.0.0"
  (provided (v-file/version) => "1.0.0"
            (v-git/version) => nil))

(fact "default version is returned if neither git nor cache file is unavailable"
  (version) => "unknown"
  (provided (v-file/version) => nil
            (v-git/version) => nil))
