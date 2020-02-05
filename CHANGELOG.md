# lein-v News -- history of user-visible changes

## 7.2.0 / 2020-02-05

* Add ability to override the marker for lein-v injected versions in dependencies.

## 7.1.0 / 2019-03-22

* Implement `push-tags` via `git push --tags` to circumvent limitation in `lein vcs push` which tries to push all commit objects and thus requires that the current branch be up-to-date.

## 7.0.0 / 2018-12-17

* Remove implicit hooks (add `abort-when-not-anchored` as required in aliases or deploy tasks)
* Remove implicit middleware (add `version-from-scm`)
* Improve subtask behavior
* Update dependencies
