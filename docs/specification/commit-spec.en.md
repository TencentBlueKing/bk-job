# Code Commit Convention

English | [简体中文](commit-spec.md)

## Code Review Recommendation
The BK-JOB project refers to multiple languages (JavaScript/Java/Python). By default, our dev team uses the following tool for code review. In order to improve the efficiency of Merge, please do a self-check before submission.
- ESLint
- Based on IntelliJ Idea's default coding style, set Continuation indent to 4. Set other parameters to default.


## Commit Formats

Commit message convention for personal fork

```
type:messsge issue
```

* type: range info
  * feature: new feature
  * fix: bug fixes
  * docs: file changes
  * style: (format, without semicolon, etc.; no changes to codes)
  * refactor: code reconstruction
  * test: Adds missing tests; reconstruction test; no changes to codes
  * chore: Codes related to script or task creation.
* message: Description of the commit 
* issue: The issue id of the commit

## Merge Request/Pull Request Suggestions

The developers might have some commit message on their forks. It is recommended to simplify commit with Git Rebase before submitting Merge Requests. For more information on simplification, please check
the previous chapter. The detailed process is listed below:

```shell
# Developing with a new fork
git checkout feature1-pick
#Multiple debugging and commits
git commit -m "xxx"
git commit -m "yyy"
git commit -m "zzz"
# When introducing a third party app, use DEP to manage it.
dep ensure -v -add github.com/org/project

# Rebase and merge multiple changes (up to 3 times), under the feature1-pick fork.
# Fill in the standard commit message again.
git rebase -i HEAD~3

# Push to the far end of the storage
git push origin feature1-pick:feature1-pick

# Submit PR/MR and wait for the merge
#......................
#......................

# After PR/MR merge, follow it with the local master fork
git fetch upstream
git rebase upstream/master
```
