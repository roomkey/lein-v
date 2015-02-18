(ns leiningen.v
  "Enrich project with SCM workspace status"
  (:require [clojure.string :as string]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.v.version :as version]
            [leiningen.v.version.protocols :refer :all]
            [leiningen.v.maven]
            [leiningen.deploy]
            [leiningen.release]
            [robert.hooke]))

(defn- version
  "Determine the version for the project by dynamically interrogating the environment"
  [{parser :parser default :default :or {default leiningen.v.maven/default parser leiningen.v.maven/parse}}]
  (let [[base distance sha dirty?] (git/version)
        parser (eval parser)]
    (when (not dirty?)
      (binding [leiningen.v.version/*parser* parser]
        (if base
          (cond-> (leiningen.v.version/parse base)
            (pos? distance) (-> (move distance)
                                (identify sha)))
          (identify default (git/sha)))))))

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
  [{config :v :as project} & [op]]
  (let [v (version config)
        op (or op leiningen.release/*level*)
        args (string/split (name op) #"-")
        v-new (apply leiningen.v.version/update v (keyword (first args)) (rest args))]
    (when (not= v v-new) (git/tag (str v-new)))
    v-new))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  ;; NB this will return true for projects without a :workspace key
  ;; TODO: handle case where #'add-workspace-data is not in configured middleware
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
    "assert-anchored" (assert (anchored? project) "Workspace is not clean and pushed to remote")
    (let [{:keys [version workspace]} project]
      (println (format "Effective version: %s, SCM workspace state: %s" version workspace)))))

;; Hooks
(defn deploy-when-anchored
  "Abort deploys unless workspace is anchored"
  []
  (robert.hooke/add-hook #'leiningen.deploy/deploy when-anchored-hook))

;; Middleware
(defn version-from-scm
  [project]
  (let [v (str (or (version project) "DIRTY"))]
    (-> project
        (assoc-in ,, [:version] v)
        (assoc-in ,, [:manifest "Implementation-Version"] v))))

(defn add-workspace-data
  [project]
  (if-let [wss (workspace-state project)]
    (-> project
        (assoc-in ,, [:workspace] wss)
        (assoc-in ,, [:manifest "Workspace-Description"] (:describe wss))
        (assoc-in ,, [:manifest "Workspace-Tracking-Status"] (string/join " || " (get-in wss [:status :tracking])))
        (assoc-in ,, [:manifest "Workspace-File-Status"] (string/join " || " (get-in wss [:status :files]))))
    project))
