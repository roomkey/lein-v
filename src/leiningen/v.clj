(ns leiningen.v
  "Enrich project with SCM workspace status"
  (:require [clojure.string :as string]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.compile]
            [leiningen.deploy]
            [robert.hooke]))

(defn- version
  "Determine the most appropriate version for the application,
   preferrably by dynamically interrogating the environment"
  [project]
  (or
   (git/version project)
   (file/version project)
   (str (:version project))
   "unknown"))

(defn- workspace-state
  [project]
  (git/workspace-state project))

(defn cache
  "Cache the effective version for use outside the scope of leiningen evaluation"
  [project & [dir]]
  (let [{{describe :describe} :workspace :keys [version source-paths]} project
        path (str (or dir (first source-paths)) "/version.clj")]
    (file/cache path version describe)))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  ;; NB this will return true for projects without a :workspace key
  (let [stable? (not-any? #(re-find #"\[ahead\s\d+\]" %) tracking)
        clean? (empty? files)]
    (and stable? clean?)))

(defn- update-source-hook
  "Update the cached version available to the application"
  [task & [project :as args]]
  (cache project)
  (apply task args))

(defn- when-anchored-hook
  "Run the task only when the workspace is anchored"
  [task & [project :as args]]
  (if (anchored? project)
    (apply task args)
    (println "Workspace is not anchored" (:workspace project))))

;; Plugin task.
(defn v
  "Show SCM workspace data"
  {:subtasks [#'cache]}
  [project & [subtask & other]]
  (condp = subtask
    "cache" (apply cache project other)
    (let [{:keys [version workspace]} project]
      (println (format "Effective version: %s, SCM workspace state: %s" version workspace)))))

;; Hooks
(defn add-to-source
  "Add version to source code"
  []
  (println "Caching the project version with the add-to-source hook is deprecated.  Prefer\n adding [\"v\" \"cache\"] to the :prep-tasks key in project.clj")
  (robert.hooke/add-hook #'leiningen.compile/compile update-source-hook))

(defn deploy-when-anchored
  "Abort deploys unless workspace is anchored"
  []
  (robert.hooke/add-hook #'leiningen.deploy/deploy when-anchored-hook)
  (try ;; "Attempt to add a hook preventing beanstalk deploys unless workspace is anchored"
    (eval '(do (require 'leiningen.beanstalk)
               ;; due to eval, when-anchored-hook needs to be fully qualified
               (robert.hooke/add-hook #'leiningen.beanstalk/deploy #'leiningen.v/when-anchored-hook)))
    (catch java.io.FileNotFoundException _)))

;; Middleware
(defn version-from-scm
  [project]
  (let [version (version project)]
    (-> project
        (assoc-in ,, [:version] version)
        (assoc-in ,, [:manifest "Implementation-Version"] version))))

(defn add-workspace-data
  [project]
  (if-let [wss (workspace-state project)]
    (-> project
        (assoc-in ,, [:workspace] wss)
        (assoc-in ,, [:manifest "Workspace-Description"] (:describe wss))
        (assoc-in ,, [:manifest "Workspace-Tracking-Status"] (string/join " || " (get-in wss [:status :tracking])))
        (assoc-in ,, [:manifest "Workspace-File-Status"] (string/join " || " (get-in wss [:status :files]))))
    project))
