(ns lein-v.plugin
  (:require [clojure.string :as string]
            [leiningen.v]
            [leiningen.compile]
            [leiningen.deploy]
            [robert.hooke]))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.compile/compile leiningen.v/update-cache-hook)
  (robert.hooke/add-hook #'leiningen.deploy/deploy leiningen.v/when-anchored-hook))

(defn middleware [project]
  (let [wss (leiningen.v/workspace-state project)
        version (leiningen.v/version project)]
    (-> project
       (assoc-in ,, [:version] version)
       (assoc-in ,, [:workspace] wss)
       (assoc-in ,, [:manifest "Implementation-Version"] version)
       (assoc-in ,, [:manifest "Workspace-Description"] (:describe wss))
       (assoc-in ,, [:manifest "Workspace-Tracking-Status"] (string/join " || " (get-in wss [:status :tracking])))
       (assoc-in ,, [:manifest "Workspace-File-Status"] (string/join " || " (get-in wss [:status :files]))))))
