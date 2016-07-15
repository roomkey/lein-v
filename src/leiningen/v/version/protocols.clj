(ns leiningen.v.version.protocols
  "Handle abstract versions as a set of malleable components")

(defprotocol ComparableAsVersion
  (<=> [this other] "Compare this version to the other version"))

(defprotocol IncrementableVersion
  (version++ [this] "Declare a new version by incrementing the base"))

(defprotocol IncrementableByLevel
  (levels [this] "Return the number of available levels")
  (level++ [this level] "Declare a new version by incrementing the given level of the base"))

(defprotocol Qualifiable
  (qualify [this qualifier] "Declare a new version by qualifying the base with the given qualifier string")
  (qualifier [this] "Return the qualifier, if any")
  (qualified? [this] "Is this version qualified?")
  (release [this] "Declare a new version by removing the qualifier of the base"))

(defprotocol IncrementableByQualifier
  (qualifier++ [this] "Declare a new version by incrementing the qualifier of the base"))

(defprotocol Snapshotable
  "A snapshot is a qualifier that identifies a series of development (pre-)releases with the same parent version elements and each of which replaces its predecessor"
  (snapshot [this] "Declare a new version by qualifying the base as a snapshot")
  (snapshot? [this]))

(defprotocol IndexableByDistance
  "A distance indexed version is a version that comes after a base version at a given integer distance"
  (move [this distance] "Move the version forward by the given distance")
  (base [this] "Return the base which this version references")
  (distance [this] "Return the distance from the base version"))

(defprotocol Identifiable
  "An identifier locates the version unambiguously in the SCM system.  Identity survives declared version updates"
  (identifier [this] "Get the identifier of this version")
  (identify [this id] "Set the identifier to the given string"))

(defprotocol Dirtyable
  "Dirty is the state wherein the version is polluted by uncommited/unversioned source"
  ;; Semantically, dirty versions aren't really even versions.  But they are practical for development...
  (dirty? [this] "Is this version dirty?")
  (dirty [this] "Mark this version as dirty"))
