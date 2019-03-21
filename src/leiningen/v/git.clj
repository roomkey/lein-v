(ns leiningen.v.git
  (:require [clojure.string :as string]
            [clojure.java.shell :as shell]
            [leiningen.core.main :as lein]))

(def ^:dynamic *prefix* "v")
(def ^:dynamic *min-sha-length* 4)
(def ^:dynamic *dirty-mark* "DIRTY")

(def unix-git-command "git")

(def windows? (some->> (System/getProperty "os.name")
                       (re-matches #"(?i).*windows.*")))

(def ^:private find-windows-git
  (memoize
   (fn []
     (let [{:keys [exit out err]} (shell/sh "where.exe" "git.exe")]
       (if-not (zero? exit)
         (lein/abort (format (str "Can't determine location of git.exe: 'where git.exe' returned %d.\n"
                                  "stdout: %s\n stderr: %s")
                             exit out err))
         (string/trim out))))))

(defn- git-exe []
  (if windows?
    (find-windows-git)
    unix-git-command))

(defn- git-command
  [& arguments]
  (let [cmd (conj arguments (git-exe))
        {:keys [exit out err]} (apply shell/sh cmd)]
    (if (zero? exit)
      (string/split-lines out)
      (do (lein/warn err) nil))))

(defn- root-distance
  []
  (count (git-command "rev-list" "HEAD")))

(defn- git-status []
  (git-command "status" "-b" "--porcelain"))

(defn- git-describe [prefix min-sha-length]
  (git-command "describe" "--long" "--match"
               (str prefix "*.*")
               (format "--abbrev=%d" min-sha-length)
               (str "--dirty=-" *dirty-mark*)
               "--always"))

(defn tag [v & {:keys [prefix sign] :or {prefix *prefix* sign "--sign"}}]
  (apply git-command (filter identity ["tag" sign "--annotate"
                                       "--message" "Automated lein-v release" (str prefix v)])))

;; This is needed because `lein vcs push` tries to push everything and gets rejected if branch is behind remote.
;; But at this point that is irrelevant and misleading (as there is typically no "normal" commit to push anyways).
;; The solution is the more restrictive `git push --tags`.
(defn push-tags
  "Push tags (only)"
  []
  (git-command "push" "--tags"))

(defn version
  [& {:keys [prefix min-sha-length]
      :or {prefix *prefix* min-sha-length *min-sha-length*}}]
  (let [re0 (re-pattern (format "^%s(.+)-(\\d+)-g([^\\-]{%d,})?(?:-(%s))?$"
                                prefix min-sha-length *dirty-mark*))
        re1 (re-pattern (format "^(Z)?(Z)?([a-z0-9]{%d,})(?:-(%s))?$" ; fallback when no matching tag
                                min-sha-length *dirty-mark*))]
    (when-let [v (first (git-describe prefix min-sha-length))]
      (let [[_ base distance sha dirty] (or (re-find re0 v) (re-find re1 v))]
        (let [distance (or (when distance (Integer/parseInt distance)) (root-distance))]
          [base distance sha (boolean dirty)])))))

(defn workspace-state [project & {:keys [prefix min-sha-length]
                                  :or {prefix *prefix* min-sha-length *min-sha-length*}}]
  (when-let [status (git-status)]
    {:status {:tracking (filter #(re-find #"^##\s" %) status)
              :files (remove empty? (remove #(re-find #"^##\s" %) status))}
     :describe (first (git-describe prefix min-sha-length))}))
