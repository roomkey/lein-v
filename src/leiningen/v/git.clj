(ns leiningen.v.git
  (:require [clojure.string :as string]
            [clojure.java.shell :as shell]))

(let [shell "/bin/bash"
      cmd [shell "-c"]]
  (defn- git-command
    [command]
    (let [cmd (conj cmd (str "git " command))
          {:keys [exit out]} (apply shell/sh cmd)]
      (when (zero? exit) (string/split-lines out)))))

(defn- git-status []
  (git-command "status -b --porcelain"))

(defn- git-describe []
  (git-command "describe --long --match 'v*.*' --abbrev=4 --dirty=**DIRTY**"))

(defn sha
  []
  (first (git-command "rev-parse --short=4 HEAD")))

(let [prefix "v"]
  (defn tag [v]
    (git-command (string/join " " ["tag --sign --annotate --message" (format "\"Release %s\"" v) (str prefix v)]))))

(let [re #"^v(.+)-(\d+)-g([^\*]{4,})?(\*\*DIRTY\*\*)?$"]
  (defn version
    []
    (when-let [v (first (git-describe))]
      (let [[_ base distance sha dirty] (re-find re v)]
        (when base [base (Integer/parseInt distance) sha dirty])))))

(defn workspace-state [project]
  (when-let [status (git-status)]
    {:status {:tracking (filter #(re-find #"^##\s" %) status)
              :files (remove empty? (remove #(re-find #"^##\s" %) status))}
     :describe (first (git-describe))}))
