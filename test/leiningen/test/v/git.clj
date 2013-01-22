(ns leiningen.test.v.git
  (:use [leiningen.v.git]
        [clojure.test]
        [midje.sweet]))

(fact "git version is returned"
  (version ..project..) => "1.0.0"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.0.0")))

(fact "build component is determined from distance from last version tag"
  (version ..project..) => "1.2.0-2"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.2.0-2-gfbc5"))
  (version ..project..) => "1.1-1"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.1-1-gb639")))

(fact "extra characters in SHA code don't deter us from parsing"
  (version ..project..) => "1.7.8-2116"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.7.8-2116-g29e84")))

(fact "dirty repo results in funky version"
  (version ..project..) => "**DIRTY**"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.2.0-2-gfbc5**DIRTY**"))
  (version ..project..) => "**DIRTY**"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.1**DIRTY**")))

(fact "SNAPSHOT versions ignore distance from last version"
  (version ..project..) => "1.2.0-SNAPSHOT"
  (provided (#'leiningen.v.git/git-describe) => (list "v1.2.0-SNAPSHOT-2-gfbc5")))

(fact "Missing version tag results in zero version"
  (version ..project..) => "0.0-SNAPSHOT"
  (provided (#'leiningen.v.git/git-describe) => (list "")))

(fact "Workspace state shows tracking and file status"
  (workspace-state ..project..) => (contains {:status (just {:tracking (just ["## master...origin/master [ahead 1]"])
                                                             :files (just ["?? TODO.txt"])})})
  (provided
    (#'leiningen.v.git/git-describe) => (list "v1.1.1")
    (#'leiningen.v.git/git-status) => ["## master...origin/master [ahead 1]" "?? TODO.txt"]))

(fact "Workspace state shows full describe output"
  (workspace-state ..project..) => (contains {:describe "v1.1.1-45-gb639"})
  (provided
    (#'leiningen.v.git/git-describe) => (list "v1.1.1-45-gb639")
    (#'leiningen.v.git/git-status) => []))