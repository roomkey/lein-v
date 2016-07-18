(ns unit.leiningen.v.semver
  (:require [leiningen.v.semver :refer :all]
            [leiningen.v.version.protocols :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "Can render as a string"
  (str (->SemVer [1 2] 3 "abcd" false)) => "1.2.3+0xabcd")

(fact "Can create from SCM data"
  (from-scm "1.2.0" 45 "12bc" false) => (as-string "1.2.45+0x12bc")
  (from-scm "1.2.0" 5 "bc12" true) => (as-string "1.2.5+0xbc12.DIRTY")
  (from-scm "1.2.0" 0 "12bc" false) => (as-string "1.2.0"))

(fact "Can release from unqualified to qualified version"
  (let [v (->SemVer [1 2] 3 "abcd" false)]
    (release v :patch) => (throws Exception)
    (release v :minor) => (as-string "1.3.0")
    (release v :major) => (as-string "2.0.0")
    (release v :alpha) => (throws Exception)))

(fact "tag element embodies the non-SCM data only"
  (let [v (->SemVer [1 2] 0 "abcd" false)]
    (tag v) => "1.2.0"))

(facts "Compares"
  (compare (->SemVer [1 2] 0 "abcd" false)
           (->SemVer [1 2] 0 "abcd" false)) => zero?
  (compare (->SemVer [1 2] 3 "1234" false)
           (->SemVer [1 2] 3 "2345" false)) => zero? ; yuck
  (compare (->SemVer [1 2] 0 "abcd" true)
           (->SemVer [1 2] 0 "abcd" false)) => pos?
  (compare (->SemVer [1 2] 0 "abcd" false)
           (->SemVer [1 3] 0 "abcc" false)) => neg?
  (compare (->SemVer [1 2] 0 "abcd" false)
           (->SemVer [1 2] 1 "1234" false)) => neg?
  (compare (->SemVer [1 2] 3 "abcd" false)
           (->SemVer [1 2] 5 "12ab" false)) => neg?)
