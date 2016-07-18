(ns unit.leiningen.v
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.impl :refer [->SimpleVersion]]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(def $project {:version :lein-v :v {:from-scm leiningen.v.impl/from-scm}})

(defchecker as-string
  [expected]
  (checker [actual]
           (extended-= (str actual) expected)))

(fact "git version components are injected if available"
  (version-from-scm $project) => (contains {:version "[[1 2 3] nil 3 \"abcd\" true]"})
  (provided (git/version) => ["[[1 2 3] nil]" 3 "abcd" true]))

(fact "default version is used if git base version is not available"
  (version-from-scm $project) => (contains {:version "[[0 0 0] nil 4 \"abcd\"]"})
  (provided (git/version) => [nil 4 "abcd" false]))

(fact "update task creates tag with updated version"
  (let [$project (assoc-in $project [:v :version] (->SimpleVersion [1 2 3] nil 3 "abcd" false))]
    (update $project :minor)) => (as-string "[[1 3 0] nil]")
  (provided
    (git/tag "[[1 3 0] nil]") => ..tagResult..))

(fact "update task does not allow a simple qualifier on released version"
  (let [$project (assoc-in $project [:v :version] (->SimpleVersion [1 2 3] nil 3 "abcd" false))]
    (update $project :snapshot)) => (throws java.lang.AssertionError)
  (provided
    (git/tag anything) => ..tagResult.. :times 0))

(fact "git tag is not created when tag result does not change"
  (let [$project (assoc-in $project [:v :version] (->SimpleVersion [1 2 3] ["SNAPSHOT" 0] 3 "abcd" false))]
    (update $project :snapshot)) => (as-string "[[1 2 3] [\"SNAPSHOT\" 0] 3 \"abcd\"]")
  (provided
    (git/tag anything) => ..tagResult.. :times 0))

(fact "compound operation is correctly parsed"
  (let [$project (assoc-in $project [:v :version] (->SimpleVersion [1 2 3] nil 3 "abcd" false))]
    (update $project :minor-alpha)) => (as-string "[[1 3 0] [\"alpha\" 0]]")
  (provided
    (git/tag "[[1 3 0] [\"alpha\" 0]]") => ..tagResult..))

(fact "deploy-when-anchored ensures deploy tasks are called when project is on a stable commit and clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files []}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => ..something..
  (provided (leiningen.deploy/deploy ..project..) => ..something..))

(fact "deploy-when-anchored ensures deploy tasks are not called when project is not on a stable commit"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master...origin/master [ahead 1]"]
                                                                    :files []}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => anything
  (provided (leiningen.deploy/deploy ..project..) => ..anything.. :times 0))

(fact "deploy-when-anchored ensures deploy tasks are not called when project is not clean"
  (against-background ..project.. =contains=> {:workspace {:status {:tracking ["## master"]
                                                                    :files ["?? new-file.txt"]}}})
  (do (deploy-when-anchored)
      (leiningen.deploy/deploy ..project..)) => anything
  (provided (leiningen.deploy/deploy ..project..) => ..anything.. :times 0))
