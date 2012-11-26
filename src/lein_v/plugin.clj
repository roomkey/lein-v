(ns lein-v.plugin
  (:require [leiningen.v]
            [robert.hooke]))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.compile/compile leiningen.v/update-cache-hook))

(defn middleware [project]
  (assoc-in project [:version] (leiningen.v/version)))