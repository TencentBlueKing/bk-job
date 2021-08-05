# Reviewing for BK-JOB

English | [简体中文](review.md)

We believe in the value of code review: It not only improves the quality and readability of codes, but also improves developers' professional competence and coding ability,
encouraging them to make better designs.

Here are Blueking Team's review issues and guides on PRs/MRs.

- [Welcome PRs/MRs](#Welcome PRs/MRs)
- [Code Reviewer](#Code Reviewer)
- [Details](#Details)
  - [Owned PR](#Owned PR/MR)
- [Merge](#Merge)

## Welcome PRs/MRs

Most importantly: If you have any questions or feedbacks, please let us know immediately.

Before making any commits, please check [commit-spec](./commit-spec.md)

## Code Reviewer

Although code review can slightly delay features and issue resolving, it also causes extra work for review. Therefore, Team Blueking hopes that the participants are active reviewers too,
that the reviewers have expertise in specific areas so that the efficiency can be improved.

## Details of Code Review

When PR/MR is submitted, the reviewers need to classify PR/MR, such as closing the repetitive ones, discerning and labeling simple user errors.
Confirm which reviewers with professional knowledge should be in charge of reviewing this PR/MR.

If the PR/MR is rejected, the reviewer needs to provide the initiator with detailed feedback and an explanation of its closure.

During the reviewing process, the initiator of PR/MR should actively answer the reviewer's questions and make comments on them. If necessary, it is recommended to make changes to the submitted content.

On workdays, the reviewer is supposed to deal with the PR/MR issues at once. On non-working days, there will be a delay in the handling of the issues.

### Owner of PR/MR

Participants can't be dealing with requests all the time. If an issue cannot be dealt with on time, it is recommended to give your feedback in the discussion section of PR/MR.
As the initiatior of PR/MR, the right approach is to negotiate with the reviewer and make a reasonable deadline, or discuss with other reviewers
about the transfer of PR/MR.

## Merge

PR/MR will be merged in the following situations:

* The reviewer doesn't raise any objections or opinions on rectification.
* All objections or change suggestions have been dealt with.
* At least one fork maintainer supports merging.
* There are relevant files and tests.
