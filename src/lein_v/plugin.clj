(ns lein-v.plugin
  (:require [leiningen.v]))

(defn hooks []
  (leiningen.v/activate))



