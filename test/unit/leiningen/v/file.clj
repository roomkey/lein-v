(ns unit.leiningen.v.file
  (:use [leiningen.v.file]
        [clojure.test]
        [midje.sweet]))

(defchecker valid-version-source
  [actual]
  (load-string actual)
  (and (= "..version.." (eval 'version/version))
       (= "..describe.." (eval 'version/raw-version))))


(defchecker valid-version-data-structure
  [actual]
  (let [version-edn (load-string actual)]
    (and (= "..version.." (:version version-edn))
         (= "..describe.." (:raw-version version-edn)))))

(fact "Version source code is cached"
  (cache ..path.. ..version.. ..describe..) => anything
  (provided
    (clojure.core/spit ..path.. valid-version-source) => ..ignored..))

(fact "Version data structure is cached"
      (cache-edn ..path.. ..version.. ..describe..) => anything
      (provided
       (clojure.core/spit ..path.. valid-version-data-structure) => ..ignored..))
