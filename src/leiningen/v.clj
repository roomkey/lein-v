(ns leiningen.v
  "Enrich project with SCM workspace status and introduce a new release model"
  (:refer-clojure :exclude [update])
  (:require [clojure.string :as string]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.v.version.protocols :refer :all]
            [leiningen.v.maven :as default-impl]
            [leiningen.deploy]
            [leiningen.core.main :refer [info warn debug]]
            [leiningen.release]
            [robert.hooke]))

(defn- version
  "Determine the version for the project by dynamically interrogating the environment"
  [{from-scm :from-scm default :default
    :or {default default-impl/default
         from-scm default-impl/from-scm}}]
  (if-let [scm (git/version)]
    (apply from-scm scm)
    default))

(defn- workspace-state
  [project]
  (git/workspace-state project))

(defn cache
  "Cache the effective version for use outside the scope of leiningen evaluation"
  [project & [dir]]
  (let [{{describe :describe} :workspace :keys [version source-paths]} project
        path (str (or dir (first source-paths)) "/version.clj")]
    (file/cache path version describe)))

(defn- update*
  "Declare an updated (newer or same in the case of snapshot) version based on the supplied operation"
  [version op]
  {:pre [(satisfies? leiningen.v.version.protocols/Releasable version) (keyword? op)
         (not (dirty? version))]
   :post [(= (dirty? version) (dirty? %)) ; Updated versions should retain their dirty flag
          (or (= version %) (zero? (distance %))) ; Updated versions should have a zero distance
          (= (sha version) (sha %)) ; Updated versions should retain their identity
          (satisfies? leiningen.v.version.protocols/SCMHosted %)]}
  (debug "*BEFORE: " version)
  (let [v (release version op)]
    (debug "* AFTER:" v)
    v))

(defn update
  "Returns SCM version updated per the supplied operation"
  [{config :v :as project} & [op]]
  (let [v (version config)
        op (or op leiningen.release/*level*)
        ops (map keyword (string/split (name op) #"-"))
        v' (loop [[op & ops] ops v v]
             (if op (recur ops (update* v op)) v))]
    (assert ((complement pos?) (compare v v')) (format "Operation %s did not advance the version" op))
    (when (not= v v') (git/tag (tag v')))
    v'))

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
    (leiningen.core.main/warn "Workspace is not anchored" (:workspace project))))

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
      (leiningen.core.main/info (format "Effective version: %s, SCM workspace state: %s" version workspace)))))

;; Hooks
(defn deploy-when-anchored
  "Abort deploys unless workspace is anchored"
  []
  (robert.hooke/add-hook #'leiningen.deploy/deploy when-anchored-hook))

;; Middleware
(defn version-from-scm
  [project]
  (let [v (str (or (version (:v project)) "UNKNOWN"))]
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
