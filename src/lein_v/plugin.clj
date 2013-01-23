(ns lein-v.plugin
  (:require [clojure.string :as string]
            [leiningen.v]
            [leiningen.deploy]
            [robert.hooke]))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.deploy/deploy leiningen.v/when-anchored-hook)
  (try ;; "Attempt to add a hook preventing beanstalk deploys unless workspace is anchored"
    (eval '(do (require 'leiningen.beanstalk)
               (robert.hooke/add-hook #'leiningen.beanstalk/deploy leiningen.v/when-anchored-hook)))
    (catch Exception _)))

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
