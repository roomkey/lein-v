(ns leiningen.test.v.file
  (:use [leiningen.v.file]
        [clojure.test]
        [midje.sweet]))

(defchecker valid-version-source
  [actual]
  (load-string actual)
  (and (= "..version.." (eval 'version/version))
       (= "..describe.." (eval 'version/raw-version))))

(fact "Version source code is cached"
  (cache ..path.. ..version.. ..describe..) => anything
  (provided
    (clojure.core/spit ..path.. valid-version-source) => ..ignored..))
