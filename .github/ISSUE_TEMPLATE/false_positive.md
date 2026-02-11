---
name: False positive
about: Report a false positive, a flow reported by FlowDroid that is wrong
title: ''
labels: 'false positive'
assignees: ''

---
Note that this report template should be used to report a false positive, a flow reported by FlowDroid that is wrong, i.e. not a real result.
Please examine each of the following points *carefully* so that we can help you as soon and best as possible.
**Note that the FlowDroid configuration is very relevant for this issue type. Some analysis options trade off precision for a higher performance. If possible, test using the most precise FlowDroid options.**

**Input file**
Please upload or provide a (working) link to the .class, .jar, .dex, .apk or any other input file in which the flow was found. You can drop a ZIP file right into this textbox.
This is _very_ important. In many cases, a bug triggers only on certain input files, which happen to be structured in a particular way which causes this problem. In case you do not supply the input files, we unfortunately often **cannot** help you at all.

**Describe the incorrectly flow found by FlowDroid**
If possible, use the context-sensitve path reconstructor to obtain a list of statements in the data-flow path and show the data-flow path here.
What is the source and sink in this flow?

**To reproduce**
Steps to reproduce the behavior:

Please include FlowDroid command line options you used or supply a code snippet to ease reproduction of the problem. **Please do not supply code snippets as _Screenshots_**. If possible, make sure that the supplied code is somewhat complete. It helps when the code for reproduction is somewhat minimal, but that is not necessary.

**Version information**
Which version of FlowDroid did you use?

**Additional context**
Add any other context about the problem here, which might help us to understand or solve your problem better. If there is more log output of FlowDroid, you should add that as well.
