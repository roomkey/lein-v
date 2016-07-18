(ns leiningen.v.impl
  "A simple implementation of lein-v version protocols used only for testing"
  (:import [java.lang Comparable])
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(defn- qualify*
  [[qs qn] qualifier]
  (if (= qs qualifier) [qs (inc qn)] [qualifier 0]))

(deftype SimpleVersion [subversions qualifier distance sha dirty?]
  Object
  (toString [this] (pr-str (cond-> [subversions qualifier]
                             (and distance (pos? distance)) (conj distance sha)
                             dirty? (conj (boolean dirty?)))))
  Comparable
  (compareTo [this that] (compare [(.subversions this) (or (.qualifier this) ["{" 9])
                                   (.distance this) (.dirty? this)]
                                  [(.subversions that) (or (.qualifier that) ["{" 9])
                                   (.distance that) (.dirty? that)]))
  SCMHosted
  (tag [this] (pr-str [subversions qualifier]))
  (distance [this] distance)
  (sha [this] sha)
  (dirty? [this] dirty?)
  Releasable
  (release [this level]
    (condp contains? level
      #{:major :minor :patch} (let [l ({:major 0 :minor 1 :patch 2} level)
                                    subversions (map-indexed (fn [i el] (cond (< i l) el
                                                                             (= i l) (inc el)
                                                                             (> i l) 0)) subversions)]
                                (SimpleVersion. (vec subversions) nil 0 sha dirty?))
      #{:alpha :beta :rc} (SimpleVersion. subversions (qualify* qualifier (name level)) 0 sha dirty?)
      #{:snapshot} (if (= "SNAPSHOT" (first qualifier))
                     this
                     (SimpleVersion. subversions ["SNAPSHOT" 0] 0 sha dirty?))
      #{:release} (do (assert qualifier "There is no pre-release version pending")
                      (SimpleVersion. subversions nil 0 sha dirty?))
      (throw (Exception. (str "Not a supported release operation: " level))))))

(defn from-scm
  ([] (SimpleVersion. [0 0 0] nil nil nil nil))
  ([tag distance sha dirty?]
   (if tag
     (let [[subversions qualifier] (read-string tag)]
       (SimpleVersion. subversions qualifier distance sha dirty?))
     (SimpleVersion. [0 0 0] nil distance sha dirty?))))
