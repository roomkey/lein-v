(ns leiningen.v
  (:require [leiningen.v.git :as git]
            [leiningen.v.file :as file]))

(defn version
  "Determine the most appropriate version for the application,
   preferrably by dynamically interrogating the environment"
  [project]
  (or
   (git/version project)
   (file/version project) ;; Not environmentally aware
   "unknown"))

(defn workspace-state
  [project]
  (git/workspace-state project))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  (let [stable? (not (some #(re-find #"\[ahead\s\d+\]" %) tracking))
        clean? (empty? files)]
    (and stable? clean?)))

;; Plugin task.
(defn v
  "Link project version to SCM workspace"
  ([project]
     (println (:workspace project))))

(defn update-cache-hook
  "Update the cached version available to the application"
  [task & [project :as args]]
  (file/cache project)
  (apply task args))

(defn when-anchored-hook
  "Run the task only when the workspace is anchored"
  [task & [project :as args]]
  (if (anchored? project)
    (apply task args)
    (println "Workspace is not anchored" (:workspace project))))