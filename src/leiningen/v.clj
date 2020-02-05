(ns leiningen.v
  "Enrich project with SCM workspace status and introduce a new release model"
  (:refer-clojure :exclude [update])
  (:require [clojure.string :as string]
            [leiningen.v.git :as git]
            [leiningen.v.file :as file]
            [leiningen.v.version.protocols :refer :all]
            [leiningen.v [maven] [semver]]
            [leiningen.deploy]
            [leiningen.core.main :refer [info warn debug abort]]
            [leiningen.release]
            [robert.hooke]))

(defn- version
  "Determine the version for the project by dynamically interrogating the environment"
  [{from-scm :from-scm :or {from-scm 'leiningen.v.maven/from-scm}}]
  (let [f (ns-resolve *ns* from-scm)
        scm (git/version)]
    (when-not scm (leiningen.core.main/warn "No SCM data available!"))
    (apply f scm)))

(defn- workspace-state
  [project]
  (git/workspace-state project))

(defn cache
  "Cache the effective version for use outside the scope of leiningen evaluation, after dir arguments, accepts several formats: clj, cljs or edn (spit clj format in none is supplied)"
  [project & [dir & formats]]
  (let [{{describe :describe} :workspace :keys [version source-paths]} project
        path-without-suffix (str (or dir (first source-paths)) "/version.")]
    (file/cache path-without-suffix version describe formats)))

(defn- update*
  "Returns SCM version updated (newer or same in the case of snapshot) per the supplied operation"
  [version ops]
  {:pre  [(every? keyword? ops)
          (satisfies? leiningen.v.version.protocols/Releasable version)
          (not (dirty? version))
          (pos? (distance version))]
   :post [(= (dirty? version) (dirty? %))                   ; Updated versions should retain their dirty flag
          (= (sha version) (sha %))                         ; Updated versions should retain their identity
          (or (= version %) (zero? (distance %)))           ; Updated versions should have a zero distance
          ((complement pos?) (compare version %))
          (satisfies? leiningen.v.version.protocols/SCMHosted %)]}
  (loop [[op & ops] ops v version]
    (if op
      (let [v' (release v op)]
        (debug (format "%s -[%s]-> %s" v op v'))
        (recur ops v'))
      v)))

(defn update
  "Declare and tag an updated version based on the supplied operations"
  [{config :v :as project} & [op]]
  (let [v (version config)
        op (or op leiningen.release/*level*)
        ops (map keyword (string/split (name op) #"-"))
        v' (update* v ops)]
    (when (not= v v') (apply git/tag (tag v') (mapcat identity config)))
    v'))

(defn- anchored? [{{{:keys [tracking files]} :status} :workspace :as project}]
  ;; NB this will return true for projects without a :workspace key
  ;; TODO: handle case where #'add-workspace-data is not in configured middleware
  (let [stable? (not-any? #(re-find #"\[ahead\s\d+\]" %) tracking)
        clean? (empty? files)]
    (and stable? clean?)))

(defn assert-anchored
  "Assert the workspace has been pushed and is clean; in principle, the project is thus reproducible"
  [project]
  (assert (anchored? project) "Workspace is not clean and pushed to remote"))

(defn- when-anchored-hook
  "Run the task only when the workspace is anchored"
  [task & [project :as args]]
  (if (anchored? project)
    (apply task args)
    (leiningen.core.main/warn "Workspace is not anchored" (:workspace project))))

(defn abort-when-not-anchored
  [project & [subtask & other]]
  (when (not (anchored? project)) (abort "Workspace is not anchored" (str (:workspace project)))))

(defn push-tags
  "Attempt to push tags regardless of freshness of current branch at default remote"
  [project]
  (git/push-tags))

;; Plugin task.
(defn v
  "Show SCM workspace data"
  {:subtasks [#'abort-when-not-anchored #'assert-anchored #'cache #'push-tags #'update]}
  [project & [subtask & other]]

  (case subtask
    "abort-when-not-anchored" (apply abort-when-not-anchored project other)
    "assert-anchored" (apply assert-anchored project other)
    "cache" (apply cache project other)
    "push-tags" (apply push-tags project other)
    "update" (apply update project other)
    nil (let [{:keys [version workspace]} project]
          (leiningen.core.main/info (format "Effective version: %s, SCM workspace state: %s" version workspace)))
    (leiningen.core.main/warn "Unrecognized subtask" subtask)))

;; Hooks
(defn ^:deprecated deploy-when-anchored
  "Abort deploys unless workspace is anchored"
  []
  (robert.hooke/add-hook #'leiningen.deploy/deploy when-anchored-hook))

;; Middleware
(defn version-from-scm
  [project]
  (let [v (str (or (version (:v project)) "UNKNOWN"))
        vk (str (or (:manifest-version-name (:v project)) "Implementation-Version"))]
    (-> project
        (assoc-in [:version] v)
        (assoc-in [:manifest vk] v))))

;; https://github.com/technomancy/leiningen/blob/stable/doc/FAQ.md
;; Since leiningen 2.4.1, this is a less intrusive way of finding the version of the application:
;;
;; (let [pom-properties (with-open [pom-properties-reader (io/reader (io/resource "META-INF/maven/x/x/pom.properties"))]
;;                        (doto (java.util.Properties.)
;;                          (.load pom-properties-reader)))]
;;   (get pom-properties "version"))

(defn- update-dependency [project v d]
  (let [lein-v-dep-flag (get-in project [:v :lein-v-dependency-flag])]
    (if (= lein-v-dep-flag (second d))
      (assoc d 1 v)
      d)))

(defn dependency-version-from-scm
  [project]
  (let [v (str (or (version (:v project)) "UNKNOWN"))]
    (clojure.core/update project
                         :dependencies
                         #(map (partial update-dependency project v) %1))))

(defn add-workspace-data
  [project]
  (if-let [wss (workspace-state project)]
    (-> project
        (assoc-in [:workspace] wss)
        (assoc-in [:manifest "Workspace-Description"] (:describe wss))
        (assoc-in [:manifest "Workspace-Tracking-Status"] (string/join " || " (get-in wss [:status :tracking])))
        (assoc-in [:manifest "Workspace-File-Status"] (string/join " || " (get-in wss [:status :files]))))
    project))
