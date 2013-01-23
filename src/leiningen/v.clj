(ns leiningen.v
  "Enrich project with SCM workspace status"
  (:require [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.compile]
            [robert.hooke]))

(defn version
  "Determine the most appropriate version for the application,
   preferrably by dynamically interrogating the environment"
  [project]
  (or
   (git/version project)
   (file/version project)
   (:version project)
   "unknown"))

(defn workspace-state
  [project]
  (git/workspace-state project))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  ;; NB this will return true for projects without a :workspace key
  (let [stable? (not (some #(re-find #"\[ahead\s\d+\]" %) tracking))
        clean? (empty? files)]
    (and stable? clean?)))

(defn- update-source-hook
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

;; Plugin task.
(defn v
  "Show SCM workspace data"
  ([project]
     (println (:workspace project))))

;; Manual hook
(defn add-to-source
  "Add version to source code"
  []
  (robert.hooke/add-hook #'leiningen.compile/compile update-source-hook))