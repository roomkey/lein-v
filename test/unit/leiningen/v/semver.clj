(ns unit.leiningen.v.semver
  (:require [leiningen.v.semver :refer [parse ->SemanticVersion]]
            [leiningen.v.version.protocols :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "Can render as a string"
  (str (->SemanticVersion [1 2 3] ["qualifier" "5"] ["metadata" "6"])) => "1.2.3-qualifier.5+metadata.6")

(fact "Can create"
  (parse "1.2.3-qualifier.5+metadata.6") => (partial instance? leiningen.v.semver.SemanticVersion))

(fact "Can increment levels"
  (levels (parse "1.2.3")) => 3
  (level++ (parse "1.2.3") 2) => (as-string "1.2.4")
  (level++ (parse "1.2.3") 1) => (as-string "1.3.0")
  (level++ (parse "1.2.3") 0) => (as-string "2.0.0")
  (level++ (parse "1.2.3") 5) => (throws java.lang.AssertionError))

(fact "Can qualify"
  (qualify (parse "1.2.3") "alpha") => (as-string "1.2.3-alpha")
  (qualifier (parse "1.2.3-q")) => "q"
  (qualified? (parse "1.2.3-q")) => truthy
  (qualified? (parse "1.2.3")) => falsey
  (release (parse "1.2.3-qualified")) => (as-string "1.2.3"))

(fact "Can increment qualifiers"
  (qualifier++ (parse "1.2.3-alpha")) => (as-string "1.2.3-alpha.2")
  (qualifier++ (parse "1.2.3-alpha.2")) => (as-string "1.2.3-alpha.3")
  (qualifier++ (parse "1.2.3")) => (throws java.lang.AssertionError))

(fact "Can manage metadata"
  (identify (parse "1.2.3") "abcd") => (as-string "1.2.3+abcd")
  (identifier (parse "1.2.3")) => nil?
  (identifier (parse "1.2.3+ab12")) => "ab12"
  (clear-identifier (parse "1.2.3+ab12")) => (as-string "1.2.3"))
