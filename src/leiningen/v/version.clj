(ns leiningen.v.version
  "Parse version numbers into malleable components"
  (:require [clojure.string :as string]
            [version-clj.core :as ver]
            [leiningen.v.version.protocols :refer :all]
            [leiningen
             [release :as release]
             [deploy :as deploy]
             [vcs :as vcs]
             [change :as change]]))

(def ^:dynamic *parser*)

(let [qualifiers [#"(?i)a(lpha)?\d*"
                  #"(?i)b(eta)?\d*"
                  #"(?i)m(ilestone)?\d*"
                  #"(?i)(rc|cr)\d*"
                  #"(?i)snapshot\d*"
                  #"(?i)ga|final|^$"
                  #"(?i)sp\d*"]]
  (defn- qindex
    [q]
    (or (first (keep-indexed #(when (re-matches %2 q) %1) qualifiers))
        (count qualifiers))))

(defn- qualify*
  [v q]
  (let [i (qindex (qualifier v))
        j (qindex q)]
    (assert (<= i j) (format "A later qualifier is pending for version %s" (str v)))
    (if (= (qualifier v) q)
      (qualifier++ v)
      (qualify v q))))

(defn parse
  [vstring]
  (*parser* vstring))

(defn update-version
  [update version distance sha]
  (let [version (parse version)
        [op qualifier] (string/split update #"\s")
;;        _ (println "****** " op qualifier " *******")
        vresult (case op
                  "major" (do (assert (not (qualified? version))
                                      (format "Pre-release version %s is pending" (str version)))
                              (cond-> (level++ version 0)
                                qualifier (qualify qualifier)))
                  "minor" (do (assert (not (qualified? version))
                                      (format "Pre-release version %s is pending" (str version)))
                              (cond-> (level++ version 1)
                                qualifier (qualify qualifier)))
                  "patch" (do (assert (not (qualified? version))
                                      (format "Pre-release version %s is pending" (str version)))
                              (cond-> (level++ version 2)
                                qualifier (qualify qualifier)))
                  "alpha" (qualify* version "alpha")
                  "beta" (qualify* version "beta")
                  "rc" (qualify* version "RC")
                  "snapshot" (if (snapshot? version)
                               version
                               (qualify* version "SNAPSHOT"))
                  "release" (do (assert (qualified? version) "There is no pre-release version pending")
                                (release version))
                  "build" (do (assert (pos? distance) "Can't overlay versions")
                              (-> version
                                  (move distance)
                                  (set-metadata sha))))]
    (str vresult)))
