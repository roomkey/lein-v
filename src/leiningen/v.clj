(ns leiningen.v
  (:require [leiningen.help]
            [leiningen.v.git]
            [leiningen.v.file]
            [leiningen.compile]
            [leiningen.core.main]
            [leiningen.core.project]
            [leiningen.test]))

(defn version
  "Determine the most appropriate version for the application,
   preferrably by dynamically interrogating the environment"
  [project]
  (or
   (leiningen.v.git/version)
   (leiningen.v.file/version) ;; Not environmentally aware
   "unknown"))

(defn workspace-state
  [project]
  (leiningen.v.git/workspace-state))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  (let [stable? (not (some #(re-find #"\[ahead\s\d+\]" %) tracking))
        clean? (empty? files)]
    (and stable? clean?)))

;; Plugin's subtasks
(defn show
  "Display the version of the Leiningen project."
  [project]
  (println (:version project)))

;; Plugin task.
(defn ^{:doc "Manage project version"
        :help-arglists '([project subtask])
        :subtasks [#'show]}
  v
  ([] (println (leiningen.help/help-for "v")))
  ([project] (v))
  ([project subtask]
     (case subtask
           "show" (show project)
           (v))))

(defn update-cache-hook
  "Update the cached version available to the application"
  [task & [project :as args]]
  (leiningen.v.file/cache project)
  (apply task args))

(defn when-anchored-hook
  "Run the task only when the workspace is anchored"
  [task & [project :as args]]
  (if (anchored? project)
    (apply task args)
    (println "Workspace is not anchored" (:workspace project))))