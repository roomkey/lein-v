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

(defprotocol SupportingMetadata
  "Metadata is additional data about the version that is not used in any computation.  Metadata never conveys to a derived version"
  (metadata [this] "Get the metadata of this version")
  (set-metadata [this m] "Set the metadata to the given string")
  (clear-metadata [this] "Remove the metadata from the version"))
