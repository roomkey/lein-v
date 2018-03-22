(ns unit.leiningen.v.file
  (:use [leiningen.v.file]
        [clojure.test]
        [midje.sweet]))

(defchecker valid-version-source-or-data-structure
  [actual]
  (let [version-edn (load-string actual)]
    (or (and (= "..version.." (eval 'version/version))
             (= "..describe.." (eval 'version/raw-version)))
        (and (= "..version.." (:version version-edn))
             (= "..describe.." (:raw-version version-edn))))))


(fact "Version source code is cached"
  (cache ..path.. ..version.. ..describe.. ..formats..) => anything
  (provided
   (clojure.core/spit (has-prefix ..path..) valid-version-source-or-data-structure) => ..ignored..))
