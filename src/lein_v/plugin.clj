(ns lein-v.plugin
  (:require [leiningen.v]
            [leiningen.compile]
            [leiningen.deploy]
            [robert.hooke]))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.compile/compile leiningen.v/update-cache-hook)
  (robert.hooke/add-hook #'leiningen.deploy/deploy leiningen.v/when-anchored-hook))

(defn middleware [project]
  (let [wss (leiningen.v/workspace-state project)]
    (-> project
       (assoc-in ,, [:version] (leiningen.v/version project))
       (assoc-in ,, [:workspace] wss))))
