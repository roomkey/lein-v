(ns unit.leiningen.v.version
  (:refer-clojure :exclude [update])
  (:require [leiningen.v.maven]
            [leiningen.v.version :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(background (around :facts (binding [leiningen.v.version/*parser* leiningen.v.maven/parse]
                             ?form)))

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "Can increment a version"
  (update (parse "0.0.0") :patch) => (as-string "0.0.1")
  (update (parse "0.0.1") :minor) => (as-string "0.1.0")
  (update (parse "0.1.1") :major) => (as-string "1.0.0"))

(fact "Can qualify as an alpha version"
  (update (parse "1.0.0") :minor "alpha") => (as-string "1.1.0-alpha"))

(fact "Can increment a qualified version"
  (update (parse "1.1.0-alpha") :alpha) => (as-string "1.1.0-alpha2"))

(fact "Can bump a qualified version"
  (update (parse "1.1.0-alpha2") :beta) => (as-string "1.1.0-beta"))

(fact "Can release a snapshot version"
  (update (parse "1.1.0-beta") :snapshot) => (as-string "1.1.0-SNAPSHOT")
  (update (parse "1.1.0") :minor "snapshot") => (as-string "1.2.0-SNAPSHOT")
  (update (parse "1.2.0-SNAPSHOT") :snapshot) => (as-string "1.2.0-SNAPSHOT"))

(fact "Can release a qualified version"
  (update (parse "1.1.0-SNAPSHOT") :release) => (as-string "1.1.0"))

(fact "Can't decrement qualifiers"
  (update (parse "1.2.3-beta3-4") :alpha) => (throws java.lang.AssertionError)
  (update (parse "1.2.3") :alpha) => (throws java.lang.AssertionError))

(fact "Can't drop qualifiers without an intermediate release"
  (update (parse "1.2.3-beta3") :major) => (throws java.lang.AssertionError))
