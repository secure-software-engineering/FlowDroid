package soot.jimple.infoflow.collections;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.collections.codeOptimization.ConstantTagFolding;
import soot.jimple.infoflow.collections.codeOptimization.StringResourcesResolver;
import soot.jimple.infoflow.collections.problems.rules.CollectionRulePropagationManagerFactory;
import soot.jimple.infoflow.collections.strategies.containers.ConstantMapStrategy;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * Class that sets up the correct classes for analysis with precise collection handling
 */
public class CollectionsSetupApplication extends SetupApplication {
    public CollectionsSetupApplication(InfoflowAndroidConfiguration config) {
        super(config);
        commonInit();
    }

    public CollectionsSetupApplication(String androidJar, String apkFileLocation) {
        super(androidJar, apkFileLocation);
        commonInit();
    }

    public CollectionsSetupApplication(String androidJar, String apkFileLocation, IIPCManager ipcManager) {
        super(androidJar, apkFileLocation, ipcManager);
        commonInit();
    }

    public CollectionsSetupApplication(InfoflowAndroidConfiguration config, IIPCManager ipcManager) {
        super(config, ipcManager);
        commonInit();
    }

    protected class CollectionsInPlaceInfoflow extends InPlaceInfoflow {

        public CollectionsInPlaceInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory, Collection<SootMethod> additionalEntryPointMethods) {
            super(androidPath, forceAndroidJar, icfgFactory, additionalEntryPointMethods);
        }

        @Override
        protected IPropagationRuleManagerFactory initializeRuleManagerFactory() {
            return new CollectionRulePropagationManagerFactory();
        }
    }

    private void commonInit() {
        addOptimizationPass(new SetupApplication.OptimizationPass() {
            @Override
            public void performCodeInstrumentationBeforeDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {
                ConstantTagFolding ctf = new ConstantTagFolding();
                ctf.initialize(manager.getConfig());
                ctf.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());

                StringResourcesResolver res = new StringResourcesResolver();
                res.initialize(manager.getConfig());
                res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
            }

            @Override
            public void performCodeInstrumentationAfterDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {

            }
        });
    }

    protected IInPlaceInfoflow createInfoflowInternal(Collection<SootMethod> lifecycleMethods) {
        final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
        return new CollectionsInPlaceInfoflow(androidJar, forceAndroidJar, cfgFactory, lifecycleMethods);
    }
}
