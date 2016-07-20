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

(defn- root-distance
  []
  (count (git-command (format "rev-list HEAD"))))

(defn- git-status []
  (git-command "status -b --porcelain"))

(defn- git-describe [prefix min-sha-length]
  (git-command (format "describe --long --match '%s*.*' --abbrev=%d --dirty=-%s --always"
                       prefix min-sha-length *dirty-mark*)))

(defn tag [v & {:keys [prefix sign] :or {prefix *prefix* sign "--sign"}}]
  (let []
    (git-command
     (string/join " " (filter identity ["tag" sign "--annotate"
                                        "--message" "\"Automated lein-v release\"" (str prefix v)])))))

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
