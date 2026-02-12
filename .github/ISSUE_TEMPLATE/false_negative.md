---
name: False negative
about: Report a false negative, a flow that was not found by FlowDroid
title: ''
labels: 'false negative'
assignees: ''

---
Note that this report template should be used to report a false negative, a flow reported that exists in the application that was not found by FlowDroid.
Please examine each of the following points *carefully* so that we can help you as soon and best as possible.
When your flow involves implicit flows (i.e. taints on ```if/loop``` conditions), you'll need to set the implicit flow mode via ```InfoflowConfiguration.setImplicitFlowMode```
Furthermore, in cases of very complex data flows, certain cut-offs such as the ```maxPathLength``` in ```PathConfiguration``` might cause flows to not be found.
**Therefore, information about the FlowDroid configuration is very important for this issue type.**

**Input file**
Please upload or provide a (working) link to the .class, .jar, .dex, .apk or any other input file in which the flow was found. You can drop a ZIP file right into this textbox.
This is _very_ important. In many cases, a bug triggers only on certain input files, which happen to be structured in a particular way which causes this problem. In case you do not supply the input files, we unfortunately often **cannot** help you at all.

**Describe the flow that was not found by FlowDroid**
What is the source and sink in the flow? Ideally, post code snippets of the complete data flow, e.g. in Java or Jimple.

If the answer of the following two questions is unclear or you are in doubt, just state "Unsure".

Are the methods that contain the source and sink statements considered reachable by the entrypoint of the program?
Note that FlowDroid will only search for leaks when the source and sink statements are considered reachable, i.e. are present in the call graph.
Was the presence of the flow in the application verified, e.g. by using some sort of dynamic analysis?

**To reproduce**
Steps to reproduce the behavior:

Please include FlowDroid command line options you used or supply a code snippet to ease reproduction of the problem. **Please do not supply code snippets as _Screenshots_**. If possible, make sure that the supplied code is somewhat complete. It helps when the code for reproduction is somewhat minimal, but that is not necessary.

**Version information**
Which version of FlowDroid did you use?

**Additional context**
Add any other context about the problem here, which might help us to understand or solve your problem better. If there is more log output of FlowDroid, you should add that as well.
