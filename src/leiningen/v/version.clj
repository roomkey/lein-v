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

(defn- qualify*
  [v q]
  (if (= (qualifier v) q)
    (qualifier++ v)
    (qualify v q)))

(defn parse
  [vstring]
  (*parser* vstring))

(defn update
  [version op & args]
  {:pre [(satisfies? leiningen.v.version.protocols/IncrementableByLevel version) (keyword? op)]
   :post [(satisfies? leiningen.v.version.protocols/IncrementableByLevel %) (neg? (compare version %))]}
  (cond
    (#{:major :minor :patch} op) (let [q (when (seq args) (string/lower-case (first args)))]
                                   (assert (not (qualified? version))
                                           (format "Pre-release version %s is pending" (str version)))
                                   (cond-> (level++ version (op {:major 0 :minor 1 :patch 2}))
                                     (#{"alpha" "beta" "rc" "snapshot"} q) (qualify q)))
    (#{:alpha :beta :rc} op) (qualify* version (string/lower-case (name op)))
    (#{:snapshot} op) (if (snapshot? version)
                        version
                        (qualify* version "SNAPSHOT"))
    (#{:release} op) (do (assert (qualified? version) "There is no pre-release version pending")
                         (release version))))
