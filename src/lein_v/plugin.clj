(ns lein-v.plugin
  (:use [leiningen.v]))

(defn hooks []
  (deploy-when-anchored))

(defn middleware [{version :version :as project}]
  (when-not (= :lein-v version)
    (println "WARNING: Future versions of lein-v will not manage this project's version automatically.  Set version string in project.clj to :lein-v to ensure future compatibility."))
  (add-workspace-data (version-from-scm project)))
