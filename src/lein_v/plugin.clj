(ns lein-v.plugin
  (:require [clojure.string :as string]
            [leiningen.v]))

(defn hooks []
  (leiningen.v/deploy-when-anchored))

(defn middleware [project]
  (let [version (leiningen.v/version project)
        project (-> project
                    (assoc-in ,, [:version] version)
                    (assoc-in ,, [:manifest "Implementation-Version"] version))]
    (if-let [wss (leiningen.v/workspace-state project)]
      (-> project
          (assoc-in ,, [:workspace] wss)
          (assoc-in ,, [:manifest "Workspace-Description"] (:describe wss))
          (assoc-in ,, [:manifest "Workspace-Tracking-Status"] (string/join " || " (get-in wss [:status :tracking])))
          (assoc-in ,, [:manifest "Workspace-File-Status"] (string/join " || " (get-in wss [:status :files]))))
      project)))
