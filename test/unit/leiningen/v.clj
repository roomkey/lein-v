(ns unit.leiningen.v
  (:refer-clojure :exclude [update])
  (:require [leiningen.v :refer :all]
            [leiningen.v.git :as git]
            [leiningen.v.impl]
            [leiningen.deploy]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [midje.checking.core :refer [extended-=]]))

(def $project {:version :lein-v :v {:from-scm 'leiningen.v.impl/from-scm}})

(def $project-with-dependencies (assoc $project
                                 :dependencies [["abc" "1.0"]
                                                ["def" nil]]))


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

(fact "git version components are injected into dependencies if available"
      (:dependencies (dependency-version-from-scm $project-with-dependencies)) => (contains [["def" "[[1 2 3] nil 3 \"abcd\" true]"]])
      (provided (git/version) => ["[[1 2 3] nil]" 3 "abcd" true]))

(fact "default version is used for dependencies if git base version is not available"
      (:dependencies (dependency-version-from-scm $project-with-dependencies)) => (contains [["def" "[[0 0 0] nil 4 \"abcd\"]"]])
      (provided (git/version) => [nil 4 "abcd" false]))


(fact "tag is created with updated version"
  (update $project :minor) => (as-string "[[1 3 0] nil]")
  (provided
    (git/version) => ["[[1 2 3] nil]" 3 "abcd" false]
    (git/tag "[[1 3 0] nil]" (as-checker keyword?) anything) => ..tagResult..))

(fact "Simple qualifier on released version is not allowed"
  (update $project :snapshot) => (throws java.lang.AssertionError)
  (provided
   (git/version) => ["[[1 2 3] nil]" 3 "abcd" false]
   (git/tag anything) => ..tagResult.. :times 0))

(fact "git tag is not created when tag result does not change"
  (update $project :snapshot) => (as-string "[[1 2 3] [\"SNAPSHOT\" 0] 3 \"abcd\"]")
  (provided
    (git/version) => ["[[1 2 3] [\"SNAPSHOT\" 0]]" 3 "abcd" false]
    (git/tag anything (as-checker keyword?) anything) => ..tagResult.. :times 0))

(fact "compound operation is correctly parsed"
  (update $project :minor-alpha) => (as-string "[[1 3 0] [\"alpha\" 0]]")
  (provided
    (git/version) => ["[[1 2 3] nil nil]" 3 "abcd" false]
    (git/tag "[[1 3 0] [\"alpha\" 0]]" (as-checker keyword?) anything) => ..tagResult..))

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
