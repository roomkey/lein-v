(ns unit.leiningen.v.git
  (:require [leiningen.v.git :refer :all]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]))

(background (around :facts (binding [*prefix* "v" *min-sha-length* 4 *dirty-mark* "D"] ?form)))

(fact "clean git version is parsed"
      (version) => (just ["1.0.0" 4 "abcd" falsey])
      (provided (#'leiningen.v.git/git-describe "v" 4) => (list "v1.0.0-4-gabcd")))

(fact "dirty repo is reflected in parsed git version"
      (version) => (just ["1.0.0" 27 "abcd" truthy])
      (provided
       (#'leiningen.v.git/git-command & anything)
       => [(format "%s1.0.0-27-g%s-%s" "v" (subs "abcdef0123456789" 0 4) *dirty-mark*)]))

(fact "git version is parsed with full (long) data"
      (version) => (just ["1.0.0" 0 "abcd" falsey])
      (provided (#'leiningen.v.git/git-describe "v" 4) => (list "v1.0.0-0-gabcd")))

(fact "missing tag and fallback is parsed"
      (version) => (just [nil 2 "abcd" false])
      (provided
       (#'leiningen.v.git/git-command "describe" & anything)
       => [(format "%s" (subs "abcdef0123456789" 0 *min-sha-length*))]
       (#'leiningen.v.git/git-command "rev-list" & anything)
       => ["aec474649bf0fdfc399d8e7a03a70821ae96d0da" "8697630e04727fe81381941e2a6b3670795f98a7"]))

(fact "dirty missing tag and fallback is parsed"
      (version) => (just [nil 2 "abcd" true])
      (provided
       (#'leiningen.v.git/git-command "describe" & anything)
       => [(format "%s-%s" (subs "abcdef0123456789" 0 *min-sha-length*) *dirty-mark*)]
       (#'leiningen.v.git/git-command "rev-list" & anything)
       => ["aec474649bf0fdfc399d8e7a03a70821ae96d0da" "8697630e04727fe81381941e2a6b3670795f98a7"]))

(fact "uncommited or nonexistant repo error is handled"
      (version) => nil?
      (provided (#'leiningen.v.git/git-describe "v" 4) => nil))

(fact "extra characters in SHA don't deter us from parsing"
      (version) => (just ["1.1.10" 33 "abcdef" falsey])
      (provided (#'leiningen.v.git/git-describe "v" 4) => (list "v1.1.10-33-gabcdef")))

(fact "Workspace state shows tracking and file status"
      (workspace-state ..project..) => (contains {:status (just {:tracking (just ["## master...origin/master [ahead 1]"])
                                                                 :files (just ["?? TODO.txt"])})})
      (provided
       (#'leiningen.v.git/git-describe "v" 4) => (list "v1.1.1")
       (#'leiningen.v.git/git-status) => ["## master...origin/master [ahead 1]" "?? TODO.txt"]))

(fact "Workspace state handles blank lines in output"
      (workspace-state ..project..) => (contains {:status (just {:tracking (just ["## master"])
                                                                 :files empty?})})
      (provided
       (#'leiningen.v.git/git-describe "v" 4) => (list "v1.1.1")
       (#'leiningen.v.git/git-status) => ["## master" ""]))

(fact "Workspace state shows full describe output"
      (workspace-state ..project..) => (contains {:describe "v1.1.1-45-gb639-D"})
      (provided
       (#'leiningen.v.git/git-describe "v" 4) => (list "v1.1.1-45-gb639-D")
       (#'leiningen.v.git/git-status) => []))

(fact "push-tags invokes git correctly"
      (push-tags) => ..whatever..
      (provided
       (#'leiningen.v.git/git-command "push" "--tags") => ..whatever..))


(fact "push-tags invokes git correctly"
      (#'leiningen.v.git/find-windows-git) => "1/git.exe"
      (provided
       (#'clojure.java.shell/sh "where.exe" "git.exe") => {:exit 0
                                                           :out "    1/git.exe
      2/git.exe
3/somewhere/anotherfolder/git.exe"
                                                           :err ""}))
