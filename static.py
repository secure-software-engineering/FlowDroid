import os

# Path to the directory containing the APK files to test
apk_dir = "/Users/ppunnun/Documents/GitHub/FlowDroid/apk"

# Path to the output directory for the leak reports
output_dir = "/Users/ppunnun/Documents/GitHub/FlowDroid/LeakReports"

# Loop over all APK files in the directory
for apk_file in os.listdir(apk_dir):
    if apk_file.endswith(".apk"):
        # Construct the path to the APK file and the output file
        apk_path = os.path.join(apk_dir, apk_file)
        report_file = os.path.join(output_dir, os.path.splitext(apk_file)[0] + ".txt")

        # Run FlowDroid with the -sink-precise option and save the output to a file
        cmd = f"java -jar soot-infoflow-cmd/target/soot-infoflow-cmd-jar-with-dependencies.jar soot.jimple.infoflow.android.TestApps.Test -a {apk_path} -p /Users/ppunnun/Library/Android/sdk/platforms/android-33/android.jar -s /Users/ppunnun/Documents/GitHub/FlowDroid/soot-infoflow-android/SourcesAndSinks.txt -sink-precise -o {report_file}"
        os.system(cmd)


