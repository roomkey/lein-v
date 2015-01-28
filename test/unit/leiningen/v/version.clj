(ns unit.leiningen.v.version
  (:require [leiningen.v.maven]
            [leiningen.v.version :refer :all]
            [midje.sweet :refer :all]))

(background (around :facts (binding [leiningen.v.version/*parser* leiningen.v.maven/parse]
                             ?form)))

(fact "Can increment a version"
  (update-version "patch" "0.0.0" 3 "abcd") => "0.0.1"
  (update-version "minor" "0.0.1" 2 "7637") => "0.1.0"
  (update-version "major" "0.1.1" 8 "83f2") => "1.0.0")

(fact "Can qualify as an alpha version"
  (update-version "minor alpha" "1.0.0" 3 "a571") => "1.1.0-alpha")

(fact "Can increment a qualified version"
  (update-version "alpha" "1.1.0-alpha" 2 "c520") => "1.1.0-alpha2")

(fact "Can release a build version on a qualified version"
  (update-version "build" "1.1.0-alpha2" 8 "bfe6") => "1.1.0-alpha2-8-0xbfe6")

(fact "Can bump a qualified version"
  (update-version "beta" "1.1.0-alpha2" 9 "c520") => "1.1.0-beta")

(fact "Can release a snapshot version"
  (update-version "snapshot" "1.1.0-beta" 1 "abfd") => "1.1.0-SNAPSHOT")

(fact "Can release a qualified version"
  (update-version "release" "1.1.0-SNAPSHOT" 5 "a12d") => "1.1.0")

(fact "Can release a build version on a simple version"
  (update-version "build" "1.1.0" 3 "fbcd") => "1.1.0-3-0xfbcd")

(fact "Can't decrement qualifiers"
  (update-version "alpha" "1.2.3-beta3-4" 3 "abcd") => (throws java.lang.AssertionError)
  (update-version "alpha" "1.2.3" 3 "abcd") => (throws java.lang.AssertionError))

(fact "Can't drop qualifiers without an intermediate release"
  (update-version "major" "1.2.3-beta3" 0 "abcd") => (throws java.lang.AssertionError))
