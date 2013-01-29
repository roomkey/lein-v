(ns lein-v.plugin
  (:use [leiningen.v]))

(defn hooks []
  (deploy-when-anchored))

(defn- select-versioner [version]
  (if (or (= :lein-v version) (and (string? version) (or (empty? version) (re-find #"lein" version))))
    (do (when-not (= :lein-v version)
          (println "WARNING: Future versions of lein-v will not manage this project's version automatically.  Set version string in project.clj to :lein-v to ensure future compatibility."))
        version-from-scm)
    identity))

(defn middleware [{version :version :as project}]
  (let [versioner (select-versioner version)]
    (add-workspace-data (versioner project))))
