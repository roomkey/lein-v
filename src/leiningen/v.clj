(ns leiningen.v
  (:require [leiningen.help]
            [leiningen.v.git]
            [leiningen.v.file]
            [leiningen.compile]
            [leiningen.core.main]
            [leiningen.core.project]
            [robert.hooke]
            [leiningen.test]))

(defn version
  "Determine the most appropriate version for the application,
   preferrably by dynamically interrogating the environment"
  []
  (or
   (leiningen.v.git/version)
   (leiningen.v.file/version) ;; Not environmentally aware
   "unknown"))

;; Plugin's subtasks
(defn show
  "Display the version of the Leiningen project."
  [project]
  (println (:version project)))

(defn cache
  "Write the version of the given Leiningen project to a file-backed cache"
  [project]
  (let [version (:version project)]
    (println (str "Caching version " version))
    (leiningen.v.file/cache project)))

;; Plugin task.
(defn ^{:doc "Manage project version"
        :help-arglists '([project subtask])
        :subtasks [#'show #'cache]}
  v
  ([] (println (leiningen.help/help-for "v")))
  ([project] (v))
  ([project subtask]
     (case subtask
           "show" (show project)
           "cache" (cache project)
           (v))))

(defn inject-version-hook [unhooked task-name project args]
  "Inject the version into the project map"
  (unhooked task-name
            (assoc-in project [:version] (leiningen.v/version))
            args))

(defn update-cache-hook
  "Update the cached version available to the application"
  [task & args]
  (let [project (first args)]
    (leiningen.v.file/cache project)
    (apply task args)))

(defn activate []
  (robert.hooke/add-hook #'leiningen.compile/compile update-cache-hook)
  (robert.hooke/add-hook #'leiningen.core.main/apply-task inject-version-hook))
