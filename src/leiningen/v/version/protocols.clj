(ns leiningen.v.version.protocols
  "Parse version numbers into malleable components")

(defprotocol ComparableAsVersion
  (<=> [this other] "Compare this version to the other version"))

(defprotocol IncrementableVersion
  (version++ [this] "Increment to the next version"))

(defprotocol IncrementableByLevel
  (levels [this] "Return incrementable levels and their ranges")
  (level++ [this level] "Increment the given level"))

(defprotocol Qualifiable
  (qualify [this qualifier] "Qualify the version with the given qualifier string")
  (qualifier [this] "Return the qualifier, if any")
  (qualified? [this] "Is this version qualified?")
  (release [this] "Remove qualifier"))

(defprotocol IncrementableByQualifier
  (qualifier++ [this] "Increment the qualifier"))

(defprotocol Snapshotable
  "A snapshot is a qualifier that identifies a series of development (pre-)releases with the same parent version elements and each of which replaces its predecessor"
  (snapshot [this] "Create a snapshot based on this version")
  (snapshot? [this]))

(defprotocol IndexableByDistance
  "A distance indexed version is a version that comes after a base version at a given integer distance"
  (move [this distance] "Move the version forward by the given distance")
  (base [this] "Return the base which this version references")
  (distance [this] "Return the distance from the base version"))

(defprotocol Identifiable
  "An identifier locates the version unambiguously in the SCM system.  Identity never conveys to a derived version"
  (identifier [this] "Get the identifier of this version")
  (identify [this id] "Set the identifier to the given string")
  (clear-identifier [this] "Remove the identifier from the version"))

(defprotocol Dirtyable
  "Dirty is the state wherein the version is polluted by uncommited/unversioned source"
  ;; Semantically, dirty versions aren't really even versions.  But they are practical for development...
  (dirty? [this] "Is this version dirty?")
  (dirty [this] "Mark this version as dirty"))
