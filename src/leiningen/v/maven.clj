;;; This implementation of lein-v versioning protocols supports incrementable versions for
;;; major, minor & patch as well as incrementable arbitrary qualifiers.  It does not support
;;; metadata.  It supports SNAPSHOT qualifiers.  There is also
;;; support for buildnumber (distance) versions.
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/index.html#
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html
;;; https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning
;;; Example Versions & Interpretations:
;;; 1.2.3-rc.4 => major 1, minor 2, patch 3, qualifier rc incremented to 4
;;; NB: java -jar ~/.m2/repository/org/apache/maven/maven-artifact/3.2.5/maven-artifact-3.2.5.jar <v1> <v2> ...<vn>
(ns leiningen.v.maven
  "An implementation of lein-v version protocols that complies with Maven v3"
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

(deftype MavenVersion [subversions qualifier build id]
  Object
  (toString [this] (let [unique (not (snapshot? this))]
                     (cond-> (string/join "." subversions)
                             qualifier (str ,, "-" (qualifier->string qualifier))
                             (and unique build) (str ,, "-" build)
                             (and unique id) (str ,, "-0x" id))))
  Comparable
  (compareTo [this other] (compare [(vec (.subversions this)) (qindex (.qualifier this)) (.build this)]
                                   [(vec (.subversions other)) (qindex (.qualifier other)) (.build other)]))
  IncrementableByLevel
  (levels [this] 3) ; TODO: let this be arbitrarily large
  (level++ [this level]
    (assert (<= 0 level (dec (.levels this))) "Not a valid level to increment")
    (let [subversions (map-indexed (fn [i el] (cond (< i level) el
                                                           (= i level) (inc el)
                                                           (> i level) 0)) subversions)]
      (MavenVersion. subversions nil nil nil)))
  Qualifiable
  ;; TODO: let qualifiers be a vector of qualifier and allow caller to specify index.  Similar, thus, to levels.
  (qualify [this qstring] (MavenVersion. subversions [qstring nil] nil nil))
  (qualifier [this] (apply str qualifier))
  (qualified? [this] qualifier)
  (release [this] (MavenVersion. subversions nil nil nil))
  Snapshotable
  (snapshot [this] (.qualify this "SNAPSHOT"))
  (snapshot? [this] (= (.qualifier this) "SNAPSHOT"))
  IncrementableByQualifier
  (qualifier++ [this]
    (assert qualifier "Can't increment non-existent qualifier")
    (let [qualifier (update-in qualifier [1] (fnil inc 1))] ; use implicit 1-based numbering
      (MavenVersion. subversions qualifier nil nil)))
  IndexableByDistance
  (move [_ distance]
    (when build (assert (<= build distance) "Can't move backwards from current version"))
    (MavenVersion. subversions qualifier distance nil))
  (base [_] (MavenVersion. subversions qualifier nil nil))
  (distance [_] build)
  Identifiable ;; Hex strings only
  (identifier [this] id)
  (identify [this id]
    (assert (re-matches #"[0-9a-f]+" id) "Identifier can only be hex strings")
    (MavenVersion. subversions qualifier build id))
  (clear-identifier [this] (MavenVersion. subversions qualifier build nil)))

(def default
  (MavenVersion. [0 0 0] nil 0 ""))

(defn parse [vstring]
  (let [[subversions & qualifiers] (map #(string/split % #"\.") (string/split vstring #"-"))
        subversions (map #(Integer/parseInt %) subversions)
        qualifiers (reduce (fn [memo [s]] (condp re-matches s
                                           #"\d+" (assoc memo :distance (Integer/parseInt s))
                                           #"0x([a-f0-9]+)" :>> #(assoc memo :sha (last %))
                                           #"\w+" (assoc memo :qualifier (string->qualifier s))))
                           {} qualifiers)
        {:keys [distance sha qualifier]} qualifiers]
    (MavenVersion. subversions qualifier distance sha)))
