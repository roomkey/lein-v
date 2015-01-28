;;; This implementation of lein-v versioning protocols supports incrementable versions for
;;; major, minor & patch as well as incrementable arbitrary qualifiers.  It also supports
;;; arbitrary string metadata.  It does not support SNAPSHOT qualifiers due to semver's insistence
;;; (section 3) on never releasing different artifacts with the same version.  There is also no
;;; support for build distance versions.  Such support would have required appropriating the patch
;;; element to represent distance (which may be a reasonable alternate implementation).
;;; http://semver.org/spec/v2.0.0.html
;;; Example Versions & Interpretations:
;;; 1.2.3-rc.4+Xabcd => major 1, minor 2, patch 3, qualifier rc incremented to 4 & metadata Xabcd

(ns leiningen.v.semver
  "An implementation of lein-v version protocols that complies with Semantic Versioning 2.0.0"
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

(deftype SemanticVersion [subversions pre-releases metadatas]
  Object
  (toString [this] (cond-> (string/join "." subversions)
                     (seq pre-releases) (str ,, "-" (extension->string pre-releases))
                     (seq metadatas) (str ,, "+" (extension->string metadatas))))
  IncrementableByLevel
  (levels [this] 3)
  (level++ [this level]
    (assert (<= 0 level (dec (.levels this))) "Not a valid level to increment")
    (let [subversions (map-indexed (fn [i el] (cond (< i level) el
                                                           (= i level) (inc el)
                                                           (> i level) 0)) subversions)]
      (SemanticVersion. subversions nil nil)))
  SupportingMetadata
  (metadata [this] (when (seq metadatas) (extension->string metadatas)))
  (set-metadata [this mstring] (SemanticVersion. subversions pre-releases (string->extension mstring)))
  (clear-metadata [this] (SemanticVersion. subversions pre-releases nil))
  Qualifiable ; semver's pre-release field is an adequate conceptual approximation
  (qualify [this qstring] (SemanticVersion. subversions (string->extension qstring) metadatas))
  (qualifier [this] (extension->string pre-releases))
  (qualified? [this] (seq pre-releases))
  (release [this] (SemanticVersion. subversions nil nil))
  IncrementableByQualifier
  (qualifier++ [this]
    (assert (seq pre-releases) "Can't increment non-existent qualifier")
    (let [pre-releases (update-in (vec pre-releases) [1] (fnil inc 1))] ; use implicit 1-based numbering
      (SemanticVersion. subversions pre-releases nil))))

(defn parse [vstring]
  (let [re #"(\d+)\.(\d+)\.(\d+)((?:-)([\w\.\-]+))?((?:\+)([\w\.\-]+))?"
        [_ major minor patch _ pre-release _ metadata] (re-matches re vstring)
        [major minor patch] (map #(Integer/parseInt %) [major minor patch])
        pre-releases (when pre-release (string->extension pre-release))
        metadatas (when metadata (string->extension metadata))]
    (SemanticVersion. [major minor patch] pre-releases metadatas)))
