(ns unit.leiningen.v.maven
  (:require [leiningen.v.maven :refer :all]
            [leiningen.v.version.protocols :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "Can render as a string"
  (str (->MavenVersion [1 2 3] ["qualifier" 5] 3 "eadaa" false)) => "1.2.3-qualifier5-3-0xeadaa")

(fact "Can create from SCM data"
  (from-scm "1.2.3-beta5" 45 "12bc" false) => (as-string "1.2.3-beta5-45-0x12bc")
  (from-scm "1.2.3" 5 "bc12" true) => (as-string "1.2.3-5-0xbc12-DIRTY")
  (from-scm "1.2.3" 0 "12bc" false) => (as-string "1.2.3"))

(fact "Can release from unqualified to qualified version"
  (let [v (->MavenVersion [1 2 3] nil 2 "abcd" false)]
    (release v :patch) => (as-string "1.2.4")
    (release v :minor) => (as-string "1.3.0")
    (release v :major) => (as-string "2.0.0")
    (release v :alpha) => (as-string "1.2.3-alpha")))

(fact "Can release from qualified to qualified version"
  (let [v (->MavenVersion [1 2 3] ["alpha" 1] 3 "abcd" false)]
    (release v :alpha) => (as-string "1.2.3-alpha2")
    (release v :beta) => (as-string "1.2.3-beta")))

(fact "Can release from qualified to unqualified version"
  (let [v (->MavenVersion [1 2 3] ["alpha" 2] 0 "abcd" false)]
    (release v :release) => (as-string "1.2.3")))

(fact "tag element embodies the non-SCM data only"
  (let [v (->MavenVersion [1 2 3] ["alpha" 1] 3 "abcd" false)]
    (tag v) => "1.2.3-alpha"))

(facts "Compares"
  (compare (->MavenVersion [1 2 3] nil 0 "abcd" false)
           (->MavenVersion [1 2 3] nil 0 "abcd" false)) => zero?
  (compare (->MavenVersion [1 2 3] nil 0 "abcd" true)
           (->MavenVersion [1 2 3] nil 0 "abcd" false)) => pos?
  (compare (->MavenVersion [1 2 3] nil 0 "abcd" false)
           (->MavenVersion [1 2 4] nil 0 "bcde" false)) => neg?
  (compare (->MavenVersion [1 2 3] nil 0 "abcd" false)
           (->MavenVersion [1 2 3] nil 1 "1234" false)) => neg?
  (compare (->MavenVersion [1 2 3] nil 3 "abcd" false)
           (->MavenVersion [1 2 3] nil 5 "12ab" false)) => neg?
  (compare (->MavenVersion [1 2 3] ["beta" 1] 0 "abcd" false)
           (->MavenVersion [1 2 3] nil 0 "def1" false)) => neg?
  (compare (->MavenVersion [1 2 3] ["alpha" 1] 0 "adcb" false)
           (->MavenVersion [1 2 3] ["beta" 1] 0 "def2" false)) => neg?
  (compare (->MavenVersion [1 2 3] nil 3 "1234" false)
           (->MavenVersion [1 2 3] nil 3 "2345" false)) => zero? ; yuck
  (compare (->MavenVersion [1 1 0] ["beta" 1] 0 "abcd" false)
           (->MavenVersion [1 1 0] ["SNAPSHOT" 1] 0 "342b" false)) => neg?
  (compare (->MavenVersion [1 1 0] ["beta" 1] 0 "3814" false)
           (->MavenVersion [1 1 0] ["beta" 2] 0 "33dc" false)) => neg?
  (compare (->MavenVersion [1 1 0] ["alpha" 2] 0 "abcd" false)
           (->MavenVersion [1 1 0] ["beta" 1] 0 "dcba" false)) => neg?)
