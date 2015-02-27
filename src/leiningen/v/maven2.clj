;;; This implementation of lein-v versioning protocols supports incrementable versions for
;;; major, minor & patch as well as incrementable arbitrary qualifiers.  It does not support
;;; metadata.  It supports SNAPSHOT qualifiers.  There is also
;;; support for buildnumber (distance) versions.
;;; http://mojo.codehaus.org/versions-maven-plugin/version-rules.html
;;; http://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN8855
;;; http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html#pom-reationships-sect-versions
;;; Example Versions & Interpretations:
;;; 1.2.3-rc4 => major 1, minor 2, patch 3, qualifier rc incremented to 4
;;; TODO - comparisons with special case handling for well-known qualifiers
(ns leiningen.v.maven2
  "An implementation of lein-v version protocols that complies with Maven v2"
  (:require [clojure.string :as string]
            [leiningen.v.version.protocols :refer :all]))

(defn- integerify
  "Parse the given string into a number, if possible"
  [s]
  (try (Integer/parseInt s)
       (catch java.lang.NumberFormatException _ s)))

(defn- string->extension
  [estring]
  (map integerify (string/split estring #"\.")))

(defn- extension->string
  [extension]
  (string/join "." extension))

(deftype Maven2Version [subversions qualifier build]
  Object
  (toString [_] (cond-> (string/join "." subversions)
                  qualifier (str ,, "-" qualifier)
                  build (str ,, "-" build)))
  IncrementableByLevel
  (levels [_] 3)
  (level++ [this level]
    (assert (<= 0 level (dec (.levels this))) "Not a valid level to increment")
    (let [subversions (map-indexed (fn [i el] (cond (< i level) el
                                                   (= i level) (inc el)
                                                   (> i level) 0)) subversions)]
      (Maven2Version. subversions nil nil)))
  Qualifiable
  (qualify [_ qstring]
    (assert (not build) "Can't use build and qualifier simultaneously")
    (Maven2Version. subversions qstring nil))
  (qualifier [_] qualifier)
  (qualified? [_] qualifier)
  (release [_] (Maven2Version. subversions nil nil))
  IncrementableByQualifier
  (qualifier++ [_]
    (assert qualifier "Can't increment non-existent qualifier")
    (let [[_ base i] (re-matches #"(\D+)(\d)?" qualifier)
          i (when i (- (int (first i)) (int \0))) ; => max revisions = 9
          qualifier (str base (inc (or i 1)))] ; use implicit 1-based numbering
      (Maven2Version. subversions qualifier build)))
  Snapshotable
  (snapshot [this] (.qualify this "SNAPSHOT"))
  (snapshot? [_] (= "SNAPSHOT" qualifier))
  IndexableByDistance
  (move [_ distance]
    (when build (assert (<= build distance) "Can't move backwards from current version"))
    (assert (not qualifier) "Can't use build and qualifier simultaneously")
    (Maven2Version. subversions nil distance))
  (base [_] (Maven2Version. subversions nil nil))
  (distance [_] build))

(defn parse
  [vstring]
  (let [re #"(\d+)\.(\d+)\.(\d+)(?:-(?:(\d+)|(\w+)))?"
        [_ major minor incremental build qualifier] (re-matches re vstring)
        subversions (map #(Integer/parseInt %) [major minor incremental])]
    (Maven2Version. subversions qualifier (when build (Integer/parseInt build)))))
