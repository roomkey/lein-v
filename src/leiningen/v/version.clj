(ns leiningen.v.version
  "Parse version numbers into malleable components"
  (:refer-clojure :exclude [update])
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(def ^:dynamic *parser*)

(defn- qualify*
  "If version is already qualified with qualifier, increment the qualifier index, otherwise advance to qualifier."
  [v q]
  (if (= (qualifier v) q)
    (qualifier++ v)
    (qualify v q)))

(defn parse
  [vstring]
  (*parser* vstring))

(defn update
  "Declare an updated (newer or same in the case of snapshot) version based on the supplied operation"
  [version op & args]
  {:pre [(satisfies? leiningen.v.version.protocols/IncrementableByLevel version) (keyword? op)]
   :post [(satisfies? leiningen.v.version.protocols/IncrementableByLevel %)
          (or (snapshot? %) (zero? (distance %))) ; Updated versions should have a zero distance
          (or (snapshot? %) (= (identifier version) (identifier %))) ; Updated versions should retain their identity
          (or (not (satisfies? Dirtyable %)) (= (dirty? version) (dirty? %))) ; Updated versions should retain their dirty flag
          ((complement pos?) (compare version %))]}
  (cond
    (#{:major :minor :patch} op) (let [q (when (seq args) (string/lower-case (first args)))]
                                   (assert (not (qualified? version))
                                           (format "Pre-release version %s is pending" (str version)))
                                   (cond-> (level++ version (op {:major 0 :minor 1 :patch 2}))
                                     (#{"alpha" "beta"} q) (qualify q)
                                     (#{"rc" "snapshot"} q) (qualify (string/upper-case q))))
    (#{:alpha :beta :rc} op) (qualify* version (string/lower-case (name op)))
    (#{:snapshot} op) (if (snapshot? version)
                        version
                        (qualify* version "SNAPSHOT"))
    (#{:release} op) (do (assert (qualified? version) "There is no pre-release version pending")
                         (release version))))
