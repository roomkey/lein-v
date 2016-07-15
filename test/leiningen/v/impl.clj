(ns leiningen.v.impl
  "A simple implementation of lein-v version protocols used only for testing"
  (:import [java.lang Comparable])
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(defn- string->qualifier
  [qstring]
  (let [[_ base i] (re-matches #"(\D+)(\d)?" qstring)
        i (when i (- (int (first i)) (int \0)))]  ; => max revisions = 9
    [base i]))

(defn- qualifier->string
  [qualifier]
  (apply str qualifier))

(let [qualifiers [#"(?i)a(lpha)?\d*"
                  #"(?i)b(eta)?\d*"
                  #"(?i)m(ilestone)?\d*"
                  #"(?i)(rc|cr)\d*"
                  #"(?i)snapshot\d*"
                  #"(?i)ga|final|^$"
                  #"(?i)sp\d*"]]
  (defn- qindex
    [qualifier]
    (let [[_ q i] (re-matches #"([^\d]*)(\d*)" qualifier)
          q (string/lower-case q)
          q0 (str (or (first (keep-indexed #(when (re-matches %2 q) %1) qualifiers))
                      (count qualifiers))
                  q)
          q1 (if (string/blank? i) 1 (Integer/parseInt i))]
      [q0 q1])))

(deftype SimpleVersion [subversions qualifier qversion distance sha dirty?]
  Object
  (toString [this] (pr-str (cond-> [subversions qualifier qversion]
                             (and distance (pos? distance)) (conj distance sha)
                             dirty? (conj (boolean dirty?)))))
  Comparable
  (compareTo [this that] (compare [(.subversions this) (.qualifier this) (.qversion this) (.distance this)]
                                  [(.subversions that) (.qualifier that) (.qversion that) (.distance that)]))
  IncrementableByLevel
  (levels [this] 3) ; TODO: let this be arbitrarily large
  (level++ [this level]
    (assert (<= 0 level (dec (.levels this))) "Not a valid level to increment")
    (let [subversions (map-indexed (fn [i el] (cond (< i level) el
                                                   (= i level) (inc el)
                                                   (> i level) 0)) subversions)]
      (SimpleVersion. (vec subversions) nil nil 0 sha dirty?)))
  Qualifiable
  (qualify [this qstring] (SimpleVersion. subversions qstring 0 0 sha dirty?))
  (qualifier [this] qualifier)
  (qualified? [this] qualifier)
  (release [this] (SimpleVersion. subversions nil nil 0 sha dirty?))
  Snapshotable
  (snapshot [this] (.qualify this "SNAPSHOT"))
  (snapshot? [this] (= (.qualifier this) "SNAPSHOT"))
  IncrementableByQualifier
  (qualifier++ [this]
    (assert qualifier "Can't increment non-existent qualifier")
    (SimpleVersion. subversions qualifier (inc qversion) 0 sha dirty?))
  IndexableByDistance
  (move [_ distance]
    (SimpleVersion. subversions qualifier qversion distance sha dirty?))
  (base [_] (SimpleVersion. subversions qualifier qversion 0 nil nil))
  (distance [_] distance)
  Identifiable
  (identifier [this] sha)
  (identify [this id]
    (SimpleVersion. subversions qualifier qversion distance id dirty?))
  Dirtyable
  (dirty? [this] dirty?)
  (dirty [this] (SimpleVersion. subversions qualifier qversion distance sha true)))

(def default
  (SimpleVersion. [0 0 0] nil nil nil nil nil))

(defn parse
  "Parse a version string"
  [vstring]
  (let [[subversions qualifier qversion distance sha dirty?] (read-string vstring)]
    (assert (every? integer? subversions))
    (when qualifier (assert (string? qualifier))
          (assert ((complement pos?) qversion)))
    (SimpleVersion. subversions qualifier qversion distance sha dirty?)))
