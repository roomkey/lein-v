;;; This implementation of lein-v versioning protocols supports incrementable versions for
;;; major, minor & patch as well as incrementable arbitrary qualifiers.  It does not support
;;; metadata.  It supports SNAPSHOT qualifiers.  There is also
;;; support for buildnumber (distance) versions.
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/index.html#
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html
;;; https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning
;;; Example Versions & Interpretations:
;;; 1.2.3-rc.4 => major 1, minor 2, patch 3, qualifier rc incremented to 4
;;; TODO - comparisons with special case handling for well-known qualifiers
;;; NB: java -jar ~/.m2/repository/org/apache/maven/maven-artifact/3.2.5/maven-artifact-3.2.5.jar <v1> <v2> ...<vn>
(ns leiningen.v.maven
  "An implementation of lein-v version protocols that complies with Maven v3"
  (:import [java.lang Comparable])
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(defn- integerify
  "Parse the given string into a number, if possible"
  [s]
  (try (Integer/parseInt s)
       (catch java.lang.NumberFormatException _ s)))

(defn- string->qualifier
  [qstring]
  (let [[_ base i] (re-matches #"(\D+)(\d)?" qstring)
        i (when i (- (int (first i)) (int \0)))]  ; => max revisions = 9
    [base i]))

(defn- qualifier->string
  [qualifier]
  (apply str qualifier))

(deftype MavenVersion [subversions qualifier build metadata]
  Object
  (toString [_] (cond-> (string/join "." subversions)
                  qualifier (str ,, "-" (qualifier->string qualifier))
                  build (str ,, "-" build)
                  metadata (str ,, "-0x" metadata)))
  Comparable
  ;; TODO: implement qualifier comparison per http://maven.apache.org/ref/3.2.5/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html
  (compareTo [this other] (compare [(vec (.subversions this)) (.qualifier this) (.build this)]
                                   [(vec (.subversions other)) (.qualifier other) (.build other)]))
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
  SupportingMetadata ;; Hex strings only
  (metadata [this] metadata)
  (set-metadata [this mstring]
    (assert (re-matches #"[0-9a-f]+" mstring) "Metadata can only be hex strings")
    (MavenVersion. subversions qualifier build mstring))
  (clear-metadata [this] (MavenVersion. subversions qualifier build nil)))

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
