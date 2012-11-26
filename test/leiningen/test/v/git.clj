(ns leiningen.test.v.git
  (:use [leiningen.v.git]
        [clojure.test]
        [midje.sweet]))

(fact "git version is returned"
  (version) => "1.0.0"
  (provided (git-describe) => "v1.0.0"))

(fact "build component is determined from distance from last version tag"
  (version) => "1.2.0-2"
  (provided (git-describe) => "v1.2.0-2-gfbc5")
  (version) => "1.1-1"
  (provided (git-describe) => "v1.1-1-gb639"))

(fact "extra characters in SHA code don't deter us from parsing"
  (version) => "1.7.8-2116"
  (provided (git-describe) => "v1.7.8-2116-g29e84"))

(fact "dirty repo results in funky version"
  (version) => "**DIRTY**"
  (provided (git-describe) => "v1.2.0-2-gfbc5**DIRTY**")
  (version) => "**DIRTY**"
  (provided (git-describe) => "v1.1**DIRTY**"))

(fact "SNAPSHOT versions ignore distance from last version"
  (version) => "1.2.0-SNAPSHOT"
  (provided (git-describe) => "v1.2.0-SNAPSHOT-2-gfbc5"))

(fact "Missing version tag results in zero version"
  (version) => "0.0-SNAPSHOT"
  (provided (git-describe) => ""))