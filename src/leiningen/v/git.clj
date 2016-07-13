(ns leiningen.v.git
  (:require [clojure.string :as string]
            [clojure.java.shell :as shell]
            [leiningen.core.main :as lein]))

(def ^:dynamic *prefix* "v")
(def ^:dynamic *min-sha-length* 4)
(def ^:dynamic *dirty-mark* "DIRTY")

(let [shell "/bin/bash"
      cmd [shell "-c"]]
  (defn- git-command
    [command]
    (let [cmd (conj cmd (str "git " command))
          {:keys [exit out err]} (apply shell/sh cmd)]
      (if (zero? exit)
        (string/split-lines out)
        (do (lein/warn err) nil)))))

(defn- git-status []
  (git-command "status -b --porcelain"))

(defn- git-describe []
  (git-command (format "describe --long --match '%s*.*' --abbrev=%d --dirty=-%s --always"
                       *prefix* *min-sha-length* *dirty-mark*)))

(defn sha
  []
  (first (git-command (format "rev-parse --short=%d HEAD" *min-sha-length*))))

(let [prefix "v"]
  (defn tag [v]
    (git-command (string/join " " ["tag --sign --annotate --message" (format "\"Release %s\"" v)
                                   (str *prefix* v)]))))

(defn version
  []
  (let [re0 (re-pattern (format "^%s(.+)-(\\d+)-g([^\\-]{%d,})?(?:-(%s))?$"
                                *prefix* *min-sha-length* *dirty-mark*))
        re1 (re-pattern (format "^(Z)?(Z)?([a-z0-9]{%d,})(?:-(%s))?$"
                                *min-sha-length* *dirty-mark*))]
    (when-let [v (first (git-describe))]
      (let [[_ base distance sha dirty] (or (re-find re0 v) (re-find re1 v))]
        [base (when distance (Integer/parseInt distance)) sha (boolean dirty)]))))

(defn workspace-state [project]
  (when-let [status (git-status)]
    {:status {:tracking (filter #(re-find #"^##\s" %) status)
              :files (remove empty? (remove #(re-find #"^##\s" %) status))}
     :describe (first (git-describe))}))
