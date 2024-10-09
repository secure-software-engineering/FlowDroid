package soot.jimple.infoflow.android.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pxb.android.axml.AxmlVisitor;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlDocument;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.ApkHandler;
import soot.jimple.infoflow.android.manifest.binary.BinaryAndroidApplication;
import soot.jimple.infoflow.android.manifest.containers.EagerComponentContainer;
import soot.jimple.infoflow.android.manifest.containers.EmptyComponentContainer;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

/**
 * This class provides easy access to all data of an AppManifest.<br />
 * Nodes and attributes of a parsed manifest can be changed. A new byte
 * compressed manifest considering the changes can be generated.
 *
 * @author Steven Arzt
 * @author Stefan Haas, Mario Schlipf
 * @see <a href=
 *      "http://developer.android.com/guide/topics/manifest/manifest-intro.html">App
 *      Manifest</a>
 */
public abstract class BaseProcessManifest<A extends IActivity, S extends IService, C extends IContentProvider, B extends IBroadcastReceiver>
		implements IManifestHandler<A, S, C, B> {

	/**
	 * Factory class for creating component implementations
	 * 
	 * @author Steven Arzt
	 *
	 */
	protected interface IComponentFactory<A extends IActivity, S extends IService, C extends IContentProvider, B extends IBroadcastReceiver> {

		/**
		 * Creates a new data object for an activity inside an Android app
		 * 
		 * @param node The binary XML node that contains the activity definition
		 * @return The new activity object
		 */
		public A createActivity(AXmlNode node);

		/**
		 * Creates a new data object for a broadcast receiver inside an Android app
		 * 
		 * @param node The binary XML node that contains the broadcast receiver
		 *             definition
		 * @return The new broadcast receiver object
		 */
		public B createBroadcastReceiver(AXmlNode node);

		/**
		 * Creates a new data object for a content provider inside an Android app
		 * 
		 * @param node The binary XML node that contains the content provider definition
		 * @return The new broadcast content provider object
		 */
		public C createContentProvider(AXmlNode node);

		/**
		 * Creates a new data object for a service inside an Android app
		 * 
		 * @param node The binary XML node that contains the service definition
		 * @return The new broadcast service object
		 */
		public S createService(AXmlNode node);

	}

	/**
	 * Handler for android xml files
	 */
	protected AXmlHandler axml;
	protected ARSCFileParser arscParser;

	// android manifest data
	protected AXmlNode manifest;
	protected AXmlNode application;

	// Components in the manifest file
	protected List<AXmlNode> providers = null;
	protected List<AXmlNode> services = null;
	protected List<AXmlNode> activities = null;
	protected List<AXmlNode> aliasActivities = null;
	protected List<AXmlNode> receivers = null;

	protected IComponentFactory<A, S, C, B> factory = createComponentFactory();

	/**
	 * Processes an AppManifest which is within the file identified by the given
	 * path.
	 *
	 * @param apkPath file path to an APK.
	 * @throws IOException if an I/O error occurs.
	 */
	public BaseProcessManifest(String apkPath) throws IOException {
		this(new File(apkPath));
	}

	/**
	 * Processes an AppManifest which is within the given {@link File}.
	 *
	 * @param apkFile the AppManifest within the given APK will be parsed.
	 * @throws IOException if an I/O error occurs.
	 * @see BaseProcessManifest
	 *      {@link BaseProcessManifest#BaseProcessManifest(InputStream,ARSCFileParser)}
	 */
	public BaseProcessManifest(File apkFile) throws IOException {
		this(apkFile, ARSCFileParser.getInstance(apkFile));
	}

	/**
	 * Processes an AppManifest which is within the given {@link File}.
	 *
	 * @param apkFile    the AppManifest within the given APK will be parsed.
	 * @param arscParser The parser for the Android resource database
	 * @throws IOException if an I/O error occurs.
	 * @see BaseProcessManifest
	 *      {@link BaseProcessManifest#BaseProcessManifest(InputStream,ARSCFileParser)}
	 */
	public BaseProcessManifest(File apkFile, ARSCFileParser arscParser) throws IOException {
		if (!apkFile.exists())
			throw new RuntimeException(
					String.format("The given APK file %s does not exist", apkFile.getCanonicalPath()));

		try (ApkHandler apk = new ApkHandler(apkFile)) {
			this.arscParser = arscParser;
			try (InputStream is = apk.getInputStream("AndroidManifest.xml")) {
				if (is == null)
					throw new FileNotFoundException(String.format("The file %s does not contain an Android Manifest",
							apkFile.getAbsolutePath()));
				this.handle(is);
			}
		}
	}

	/**
	 * Processes an AppManifest which is provided by the given {@link InputStream}.
	 *
	 * @param manifestIS InputStream for an AppManifest.
	 * @param arscParser The Android resource file parser
	 * @throws IOException if an I/O error occurs.
	 */
	public BaseProcessManifest(InputStream manifestIS, ARSCFileParser arscParser) throws IOException {
		this.arscParser = arscParser;
		this.handle(manifestIS);
	}

	/**
	 * Initialises the {@link BaseProcessManifest} by parsing the manifest provided
	 * by the given {@link InputStream}.
	 *
	 * @param manifestIS InputStream for an AppManifest.
	 * @throws IOException            if an I/O error occurs.
	 * @throws XmlPullParserException can occur due to a malformed manifest.
	 */
	protected void handle(InputStream manifestIS) throws IOException {
		this.axml = new AXmlHandler(manifestIS);

		// get manifest node
		AXmlDocument document = this.axml.getDocument();
		this.manifest = document.getRootNode();
		if (!this.manifest.getTag().equals("manifest"))
			throw new RuntimeException("Root node is not a manifest node");

		// get application node
		List<AXmlNode> applications = this.manifest.getChildrenWithTag("application");
		if (applications.isEmpty())
			throw new RuntimeException("Manifest contains no application node");
		else if (applications.size() > 1)
			throw new RuntimeException("Manifest contains more than one application node");
		this.application = applications.get(0);

		// Get components
		this.providers = this.axml.getNodesWithTag("provider");
		this.services = this.axml.getNodesWithTag("service");
		this.activities = this.axml.getNodesWithTag("activity");
		this.aliasActivities = this.axml.getNodesWithTag("activity-alias");
		this.receivers = this.axml.getNodesWithTag("receiver");
	}

	/**
	 * Returns the handler which parsed and holds the manifest's data.
	 *
	 * @return Android XML handler
	 */
	public AXmlHandler getAXml() {
		return this.axml;
	}

	/**
	 * The unique <code>manifest</code> node of the AppManifest.
	 *
	 * @return manifest node
	 */
	public AXmlNode getManifest() {
		return this.manifest;
	}

	@Override
	public IAndroidApplication getApplication() {
		return new BinaryAndroidApplication(this.application, this);
	}

	@Override
	public IComponentContainer<C> getContentProviders() {
		if (this.providers == null)
			return EmptyComponentContainer.get();
		return new EagerComponentContainer<>(
				this.providers.stream().map(p -> factory.createContentProvider(p)).collect(Collectors.toList()));
	}

	@Override
	public IComponentContainer<S> getServices() {
		if (this.services == null)
			return EmptyComponentContainer.get();
		return new EagerComponentContainer<>(
				services.stream().map(s -> factory.createService(s)).collect(Collectors.toList()));
	}

	/**
	 * Gets the type of the component identified by the given class name
	 *
	 * @param className The class name for which to get the component type
	 * @return The component type of the given class if this class has been
	 *         registered as a component in the manifest file, otherwise null
	 */
	public ComponentType getComponentType(String className) {
		for (AXmlNode node : this.activities)
			if (node.getAttribute("name").asString(arscParser).equals(className))
				return ComponentType.Activity;
		for (AXmlNode node : this.services)
			if (node.getAttribute("name").asString(arscParser).equals(className))
				return ComponentType.Service;
		for (AXmlNode node : this.receivers)
			if (node.getAttribute("name").asString(arscParser).equals(className))
				return ComponentType.BroadcastReceiver;
		for (AXmlNode node : this.providers)
			if (node.getAttribute("name").asString(arscParser).equals(className))
				return ComponentType.ContentProvider;
		return null;
	}

	@Override
	public IComponentContainer<A> getActivities() {
		if (this.activities == null)
			return EmptyComponentContainer.get();
		return new EagerComponentContainer<>(
				this.activities.stream().map(a -> factory.createActivity(a)).collect(Collectors.toList()));
	}

	/**
	 * Returns a list containing all nodes with tag <code>activity-alias</code>
	 *
	 * @return list with all alias activities
	 */
	public List<AXmlNode> getAliasActivities() {
		return new ArrayList<AXmlNode>(this.aliasActivities);
	}

	@Override
	public IComponentContainer<B> getBroadcastReceivers() {
		if (this.receivers == null)
			return EmptyComponentContainer.get();
		return new EagerComponentContainer<>(
				receivers.stream().map(r -> factory.createBroadcastReceiver(r)).collect(Collectors.toList()));
	}

	/**
	 * Returns the <code>provider</code> which has the given <code>name</code>.
	 *
	 * @param name the provider's name
	 * @return provider with <code>name</code>
	 */
	public AXmlNode getProvider(String name) {
		return this.getNodeWithName(this.providers, name);
	}

	/**
	 * Returns the <code>service</code> which has the given <code>name</code>.
	 *
	 * @param name the service's name
	 * @return service with <code>name</code>
	 */
	public AXmlNode getService(String name) {
		return this.getNodeWithName(this.services, name);
	}

	/**
	 * Returns the <code>activity</code> which has the given <code>name</code>.
	 *
	 * @param name the activitie's name
	 * @return activitiy with <code>name</code>
	 */
	public AXmlNode getActivity(String name) {
		return this.getNodeWithName(this.activities, name);
	}

	/**
	 * Returns the <code>alias analysis</code> which has the given <code>name</code>
	 *
	 * @param name the alias activity's name
	 * @return alias activity with <code>name</code>
	 */
	public AXmlNode getAliasActivity(String name) {
		return this.getNodeWithName(this.aliasActivities, name);
	}

	/**
	 * Returns the <code>receiver</code> which has the given <code>name</code>.
	 *
	 * @param name the receiver's name
	 * @return receiver with <code>name</code>
	 */
	public AXmlNode getReceiver(String name) {
		return this.getNodeWithName(this.receivers, name);
	}

	/**
	 * Iterates over <code>list</code> and checks which node has the given
	 * <code>name</code>.
	 *
	 * @param list contains nodes.
	 * @param name the node's name.
	 * @return node with <code>name</code>.
	 */
	protected AXmlNode getNodeWithName(List<AXmlNode> list, String name) {
		for (AXmlNode node : list) {
			Object attr = node.getAttributes().get("name");
			if (attr != null && ((AXmlAttribute<?>) attr).getValue().equals(name))
				return node;
		}

		return null;
	}

	/**
	 * Returns the target activity specified in the <code>targetActivity</code>
	 * attribute of the alias activity
	 *
	 * @param aliasActivity
	 * @return activity
	 */
	public AXmlNode getAliasActivityTarget(AXmlNode aliasActivity) {
		if (BaseProcessManifest.isAliasActivity(aliasActivity)) {
			AXmlAttribute<?> targetActivityAttribute = aliasActivity.getAttribute("targetActivity");
			if (targetActivityAttribute != null) {
				return this.getActivity(targetActivityAttribute.asString(arscParser));
			}
		}
		return null;
	}

	/**
	 * Returns whether the given activity is an alias activity or not
	 *
	 * @param activity
	 * @return True if the activity is an alias activity, False otherwise
	 */
	public static boolean isAliasActivity(AXmlNode activity) {
		return activity.getTag().equals("activity-alias");
	}

	public ArrayList<AXmlNode> getAllActivities() {
		ArrayList<AXmlNode> allActivities = new ArrayList<>(this.activities);
		allActivities.addAll(this.aliasActivities);
		return allActivities;
	}

	/**
	 * Returns the Manifest as a compressed android xml byte array. This will
	 * consider all changes made to the manifest and application nodes respectively
	 * to their child nodes.
	 *
	 * @return byte compressed AppManifest
	 * @see AXmlHandler#toByteArray()
	 */
	public byte[] getOutput() {
		return this.axml.toByteArray();
	}

	private String cache_PackageName = null;

	/**
	 * Gets the application's package name
	 *
	 * @return The package name of the application
	 */
	public String getPackageName() {
		if (cache_PackageName == null) {
			AXmlAttribute<?> attr = this.manifest.getAttribute("package");
			if (attr != null)
				cache_PackageName = attr.asString(arscParser);
		}
		return cache_PackageName;
	}

	/**
	 * Gets the version code of the application. This code is used to compare
	 * versions for updates.
	 *
	 * @return The version code of the application
	 */
	public int getVersionCode() {
		AXmlAttribute<?> attr = this.manifest.getAttribute("versionCode");
		return attr == null ? -1 : attr.asInteger(arscParser);
	}

	/**
	 * Gets the application's version name as it is displayed to the user
	 *
	 * @return The application#s version name as in pretty print
	 */
	public String getVersionName() {
		AXmlAttribute<?> attr = this.manifest.getAttribute("versionName");
		return attr == null ? null : attr.asString(arscParser);
	}

	/**
	 * Gets the minimum SDK version on which this application is supposed to run
	 *
	 * @return The minimum SDK version on which this application is supposed to run
	 */
	public int getMinSdkVersion() {
		List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
		if (usesSdk == null || usesSdk.isEmpty())
			return -1;
		AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("minSdkVersion");
		return attr == null ? -1 : attr.asInteger(arscParser);
	}

	/**
	 * Gets the target SDK version for which this application was developed
	 *
	 * @return The target SDK version for which this application was developed
	 */
	public int getTargetSdkVersion() {
		List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
		if (usesSdk == null || usesSdk.isEmpty())
			return -1;
		AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("targetSdkVersion");
		return attr == null ? -1 : attr.asInteger(arscParser);
	}

	/**
	 * Gets the permissions this application requests
	 *
	 * @return The permissions requested by this application
	 */
	public Set<String> getPermissions() {
		List<AXmlNode> usesPerms = this.manifest.getChildrenWithTag("uses-permission");
		Set<String> permissions = new HashSet<String>();
		for (AXmlNode perm : usesPerms) {
			AXmlAttribute<?> attr = perm.getAttribute("name");
			if (attr != null)
				permissions.add(attr.asString(arscParser));
			else {
				// The required "name" attribute is missing, so we collect all
				// empty attributes as a best-effort solution for broken malware apps
				for (AXmlAttribute<?> a : perm.getAttributes().values())
					if (a.getType() == AxmlVisitor.TYPE_STRING && (a.getName() == null || a.getName().isEmpty()))
						permissions.add(a.asString());
			}
		}
		return permissions;
	}

	/**
	 * * Gets the intent-filter components this application used
	 *
	 * @return the intent filter used by this application
	 **/

	public Set<String> getIntentFilter() {
		List<AXmlNode> usesActions = this.axml.getNodesWithTag("action");
		Set<String> intentFilters = new HashSet<>();
		for (AXmlNode ittft : usesActions) {
			if (ittft.getParent().getTag().equals("intent-filter")) {
				AXmlAttribute<?> attr = ittft.getAttribute("name");
				if (attr != null) {
					intentFilters.add(attr.asString(arscParser));
				} else {
					// The required "name" attribute is missing, so we collect all
					// empty attributes as a best-effort solution for broken malware apps
					for (AXmlAttribute<?> a : ittft.getAttributes().values())
						intentFilters.add(a.asString(arscParser));
				}
			}
		}
		return intentFilters;
	}

	/**
	 * Gets the hardware components this application requests
	 *
	 * @return the hardware requested by this application
	 **/
	public Set<String> getHardware() {
		List<AXmlNode> usesHardware = this.manifest.getChildrenWithTag("uses-feature");
		Set<String> hardware = new HashSet<>();
		for (AXmlNode hard : usesHardware) {
			AXmlAttribute<?> attr = hard.getAttribute("name");
			if (attr != null) {
				hardware.add(attr.asString(arscParser));
			} else {
				// The required "name" attribute is missing, following flowdroid,
				// I also collect all empty
				// attributes as a best-effort solution for broken malware apps
				for (AXmlAttribute<?> a : hard.getAttributes().values()) {
					if (a.getType() == AxmlVisitor.TYPE_STRING && (a.getName() == null || a.getName().isEmpty())) {
						hardware.add(a.asString(arscParser).toString());
					}
				}
			}
		}
		return hardware;
	}

	/**
	 * Adds a new permission to the manifest.
	 *
	 * @param permissionName complete permission name e.g.
	 *                       "android.permission.INTERNET"
	 */
	public void addPermission(String permissionName) {
		AXmlNode permission = new AXmlNode("uses-permission", null, manifest, "");
		AXmlAttribute<String> permissionNameAttr = new AXmlAttribute<String>("name", permissionName,
				AXmlHandler.ANDROID_NAMESPACE);
		permission.addAttribute(permissionNameAttr);
	}

	/**
	 * Adds a new provider to the manifest
	 *
	 * @param node provider represented as an AXmlNode
	 */
	public void addProvider(AXmlNode node) {
		if (providers.isEmpty())
			providers = new ArrayList<AXmlNode>();
		providers.add(node);
	}

	/**
	 * Adds a new receiver to the manifest
	 *
	 * @param node receiver represented as an AXmlNode
	 */
	public void addReceiver(AXmlNode node) {
		if (receivers.isEmpty())
			receivers = new ArrayList<AXmlNode>();
		receivers.add(node);
	}

	/**
	 * Adds a new activity to the manifest
	 *
	 * @param node activity represented as an AXmlNode
	 */
	public void addActivity(AXmlNode node) {
		if (activities.isEmpty())
			activities = new ArrayList<AXmlNode>();
		activities.add(node);
	}

	/**
	 * Adds a new service to the manifest
	 *
	 * @param node service represented as an AXmlNode
	 */
	public void addService(AXmlNode node) {
		if (services.isEmpty())
			services = new ArrayList<AXmlNode>();
		services.add(node);
	}

	/**
	 * Closes this apk file and all resources associated with it
	 */
	@Override
	public void close() {
	}

	/**
	 * Returns all activity nodes that are "launchable", i.e. that are called when
	 * the user clicks on the button in the launcher.
	 *
	 * @return all launchable activity nodes
	 */
	public Set<AXmlNode> getLaunchableActivityNodes() {
		Set<AXmlNode> allLaunchableActivities = new LinkedHashSet<AXmlNode>();

		for (AXmlNode activity : this.getAllActivities()) {
			for (AXmlNode activityChildren : activity.getChildren()) {
				if (activityChildren.getTag().equals("intent-filter")) {
					boolean actionFilter = false;
					boolean categoryFilter = false;
					for (AXmlNode intentFilter : activityChildren.getChildren()) {
						AXmlAttribute<?> aname = intentFilter.getAttribute("name");
						if (aname == null)
							continue;
						String name = aname.asString(arscParser);
						String tag = intentFilter.getTag();
						if (tag.equals("action") && name.equals("android.intent.action.MAIN"))
							actionFilter = true;
						else if (tag.equals("category") && name.equals("android.intent.category.LAUNCHER"))
							categoryFilter = true;
					}

					if (actionFilter && categoryFilter)
						allLaunchableActivities.add(activity);
				}
			}

		}

		return allLaunchableActivities;
	}

	/**
	 * Generates a full class name from a short class name by appending the
	 * globally-defined package when necessary
	 *
	 * @param className The class name to expand
	 * @return The expanded class name for the given short name
	 */
	public String expandClassName(String className) {
		String packageName = getPackageName();
		if (className.startsWith("."))
			return packageName + className;
		else if (!className.contains("."))
			return packageName + "." + className;
		else
			return className;
	}

	/**
	 * Gets the Android resource parser
	 * 
	 * @return The Android resource parser
	 */
	public ARSCFileParser getArscParser() {
		return arscParser;
	}

	/**
	 * Creates a factory that can instantiate data objects for Android components
	 * 
	 * @return The new factory
	 */
	protected abstract IComponentFactory<A, S, C, B> createComponentFactory();

}
