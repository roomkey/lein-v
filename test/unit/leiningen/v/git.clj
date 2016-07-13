(ns unit.leiningen.v.git
  (:require [leiningen.v.git :refer :all]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]))

(background (around :facts (binding [*prefix* "v" *min-sha-length* 4 *dirty-mark* "D"] ?form)))

(fact "clean git version is parsed"
      (version) => (just ["1.0.0" 4 "abcd" falsey])
      (provided (#'leiningen.v.git/git-describe) => (list "v1.0.0-4-gabcd")))

(fact "dirty repo is reflected in parsed git version"
      (version) => (just ["1.0.0" 27 "abcde" truthy])
      (provided
       (#'leiningen.v.git/git-command (as-checker string?))
       => [(format "%s1.0.0-27-gabcde-%s" *prefix* *dirty-mark*)]))

(fact "git version is parsed with full (long) data"
  (version) => (just ["1.0.0" 0 "abcd" falsey])
  (provided (#'leiningen.v.git/git-describe) => (list "v1.0.0-0-gabcd")))

(fact "missing tag for git version is parsed"
  (version) => nil?
  (provided (#'leiningen.v.git/git-describe) => nil))

(fact "extra characters in SHA don't deter us from parsing"
  (version) => (just ["1.1.10" 33 "abcdef" falsey])
  (provided (#'leiningen.v.git/git-describe) => (list "v1.1.10-33-gabcdef")))

(fact "Workspace state shows tracking and file status"
  (workspace-state ..project..) => (contains {:status (just {:tracking (just ["## master...origin/master [ahead 1]"])
                                                             :files (just ["?? TODO.txt"])})})
  (provided
    (#'leiningen.v.git/git-describe) => (list "v1.1.1")
    (#'leiningen.v.git/git-status) => ["## master...origin/master [ahead 1]" "?? TODO.txt"]))

(fact "Workspace state handles blank lines in output"
  (workspace-state ..project..) => (contains {:status (just {:tracking (just ["## master"])
                                                             :files empty?})})
  (provided
    (#'leiningen.v.git/git-describe) => (list "v1.1.1")
    (#'leiningen.v.git/git-status) => ["## master" ""]))

(fact "Workspace state shows full describe output"
  (workspace-state ..project..) => (contains {:describe "v1.1.1-45-gb639-D"})
  (provided
    (#'leiningen.v.git/git-describe) => (list "v1.1.1-45-gb639-D")
    (#'leiningen.v.git/git-status) => []))
