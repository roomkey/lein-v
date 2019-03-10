(ns leiningen.v.file
  (:require [clojure.string :as string]))

(defn- cache-source
  "Return Clojure source code for the version cache file"
  [version describe]
  (string/join "\n" [";; This code was automatically generated by the 'lein-v' plugin"
                     "(ns version)"
                     (format "(def version \"%s\")" version)
                     (format "(def raw-version \"%s\")" describe)
                     ""]))

(defn- cache-edn-source
  "return EDN data structure for the version cache EDN file"
  [version describe]
  (pr-str  {:version version
            :raw-version describe}))

(defn version
  "Peek into the source of the project to read the cached version"
  [file]
  (try
    (load-file file)
    (eval 'version/version)
    (catch Exception _)))

(defn in?
  "true if coll contains e"
  [coll e]
  (some #(= e %) coll))

(defn cache
  "Write the version of the given Leiningen project to a file-backed caches in different formats: clj (always written for backwards compatibility if no formats are provided), cljs, edn"
  [path version describe formats]
  (clojure.java.io/make-parents path)
  (when (= 0 (count formats))
    (spit (str path "clj") (cache-source version describe)))
  (when (in? formats "clj") (spit (str path "clj") (cache-source version describe)))
  (when (in? formats "cljs") (spit (str path "cljs") (cache-source version describe)))
  (when (in? formats "cljc") (spit (str path "cljc") (cache-source version describe)))
  (when (in? formats "cljx") (spit (str path "cljx") (cache-source version describe)))
  (when (in? formats "edn") (spit (str path "edn") (cache-edn-source version describe))))
