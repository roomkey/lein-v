(ns leiningen.v
  "Enrich project with SCM workspace status"
  (:require [clojure.string :as string]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.v.version :as version]
            [leiningen.v.version.protocols :refer :all]
            [leiningen.v.maven]
            [leiningen.compile]
            [leiningen.deploy]
            [leiningen.release]
            [robert.hooke]))

(defn- version
  "Determine the version for the project by dynamically interrogating the environment"
  [{parser :parser default :default :or {default "0.0.1-SNAPSHOT" parser leiningen.v.maven/parse}}]
  (let [[base distance sha dirty] (git/version)
        parser (eval parser)]
    (binding [leiningen.v.version/*parser* parser]
      (if base ;; TODO: allow implementation-specific default version
        (cond-> (leiningen.v.version/parse base)
          (pos? distance) (-> (move distance)
                              (set-metadata sha)))
        (leiningen.v.version/parse default)))))

(defn- workspace-state
  [project]
  (git/workspace-state project))

(defn cache
  "Cache the effective version for use outside the scope of leiningen evaluation"
  [project & [dir]]
  (let [{{describe :describe} :workspace :keys [version source-paths]} project
        path (str (or dir (first source-paths)) "/version.clj")]
    (file/cache path version describe)))

(defn update
  "Returns project's version string updated per the supplied operation"
  [{v-str :version config :v :as project} & [op & args]]
  (let [v (version config)
        op (or op leiningen.release/*level*)]
    (git/tag (str (apply leiningen.v.version/update v op args)))))

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
  {:subtasks [#'cache #'update]}
  [project & [subtask & other]]
  (condp = subtask
    "cache" (apply cache project other)
    "update" (apply update project other)
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
  (robert.hooke/add-hook #'leiningen.deploy/deploy when-anchored-hook))

;; Middleware
(defn version-from-scm
  [project]
  (let [version (str (version project))]
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
