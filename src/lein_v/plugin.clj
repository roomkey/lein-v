(ns lein-v.plugin
  (:require [leiningen.v :refer [deploy-when-anchored version-from-scm add-workspace-data]]))

(defn hooks []
  (deploy-when-anchored))

(defn- select-versioner [version]
  (if (#{:lein-v ":lein-v"} version)
    version-from-scm
    (do (when (and (string? version) (or (empty? version) (re-find #"lein" version)))
          (leiningen.core.main/warn "WARNING: lein-v is not managing this project's version.  Set version in project.clj to :lein-v to trigger automatic lein-v management"))
        identity)))

(defn middleware [{version :version :as project}]
  (let [versioner (select-versioner version)]
    (add-workspace-data (versioner project))))
